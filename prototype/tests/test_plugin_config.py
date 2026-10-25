import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

from gateway.config import Config, PluginConfig, factories, load
from gateway.core import Context, Response


class PluginConfigTests(unittest.TestCase):
    def config(self, plugins):
        return Config.model_validate({
            "upstream": {"base_url": "http://127.0.0.1:1/v1"},
            "plugins": plugins,
        })

    def test_enabled_defaults_to_true_for_existing_configurations(self):
        plugin = PluginConfig(factory="gateway.plugins.authentication:factory")
        self.assertTrue(plugin.enabled)
        self.assertEqual(plugin.options, {})

    def test_disabled_nonexistent_factory_is_not_imported(self):
        config = self.config([{
            "factory": "gateway.plugins.nonexistent_test_plugin:factory",
            "enabled": False,
            "options": {"message": "retained"},
        }])
        with patch("gateway.config.import_module") as import_module:
            self.assertEqual(factories(config), [])
        import_module.assert_not_called()
        self.assertEqual(config.plugins[0].options, {"message": "retained"})

    def test_only_enabled_factories_are_loaded_in_config_order(self):
        config = self.config([
            {"factory": "gateway.plugins.first:factory", "options": {"message": "one"}},
            {"factory": "gateway.plugins.missing:factory", "enabled": False},
            {"factory": "gateway.plugins.last:factory", "enabled": True,
             "options": {"message": "two"}},
        ])
        first, last = Mock(), Mock()
        with patch("gateway.config.import_module", side_effect=[first, last]) as import_module:
            result = factories(config)
        self.assertEqual(
            [call.args for call in import_module.call_args_list],
            [("gateway.plugins.first",), ("gateway.plugins.last",)],
        )
        self.assertEqual(len(result), 2)
        async def next_handler(context: Context) -> Response:
            return Response({})

        result[0](next_handler)
        result[1](next_handler)
        first.factory.assert_called_once_with(next_handler, message="one")
        last.factory.assert_called_once_with(next_handler, message="two")

    def test_load_uses_environment_path_and_keeps_disabled_options(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "config.toml"
            path.write_text(
                '[upstream]\n'
                'base_url = "http://127.0.0.1:1/v1"\n\n'
                '[[plugins]]\n'
                'factory = "gateway.plugins.missing:factory"\n'
                'enabled = false\n'
                '[plugins.options]\n'
                'message = "saved while disabled"\n'
                'tags = ["one", "two"]\n',
                encoding="utf-8",
            )
            with patch.dict(os.environ, {"GATEWAY_CONFIG": str(path)}):
                config = load()
                reloaded = load()
        self.assertFalse(config.plugins[0].enabled)
        self.assertEqual(config.plugins[0].options, {
            "message": "saved while disabled", "tags": ["one", "two"],
        })
        self.assertEqual(reloaded, config)


if __name__ == "__main__":
    unittest.main()
