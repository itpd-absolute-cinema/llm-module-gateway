import unittest

from prometheus_client import CollectorRegistry

from app.metrics import MetricsRegistry


class MetricsRegistryTest(unittest.TestCase):
    def setUp(self):
        self.prom = CollectorRegistry()
        self.reg = MetricsRegistry(self.prom)

    def value(self, name, **labels):
        return self.prom.get_sample_value(name, labels) or 0.0

    def test_valid_declaration_and_values(self):
        declared = self.reg.declare("p", {
            "hits": {"type": "counter", "help": "h", "labels": {"lang": ["ru", "en"]}},
            "size": {"type": "gauge"},
            "len": {"type": "histogram", "buckets": [1, 5]},
        })
        self.assertEqual(set(declared), {"hits", "size", "len"})
        self.reg.apply("p", declared, [
            ("inc", "hits", 2, {"lang": "ru"}), ("inc", "hits", 1, {"lang": "de"}),
            ("set", "size", 7, {}), ("observe", "len", 3, {})])
        self.assertEqual(self.value("llm_plugin_hits_total", plugin="p", lang="ru"), 2)
        self.assertEqual(self.value("llm_plugin_hits_total", plugin="p", lang="other"), 1)
        self.assertEqual(self.value("llm_plugin_size", plugin="p"), 7)
        self.assertEqual(self.value("llm_plugin_len_bucket", plugin="p", le="5.0"), 1)

    def test_label_less_series_exists_from_the_start(self):
        self.reg.declare("p", {"hits": {"type": "counter"}})
        self.assertIsNotNone(self.prom.get_sample_value("llm_plugin_hits_total", {"plugin": "p"}))

    def test_invalid_declarations_are_rejected(self):
        bad = {
            "Upper": {"type": "counter"},
            "calls": {"type": "counter"},
            "x_total": {"type": "counter"},
            "no_type": {},
            "bad_type": {"type": "summary"},
            "too_many": {"type": "counter", "labels": {f"l{i}": ["a"] for i in range(5)}},
            "plugin_label": {"type": "counter", "labels": {"plugin": ["a"]}},
            "open_label": {"type": "counter", "labels": {"user": []}},
            "bad_buckets": {"type": "histogram", "buckets": [5, 1]},
            "good": {"type": "counter"},
        }
        with self.assertLogs("plugin-manager", level="WARNING"):
            declared = self.reg.declare("p", bad)
        self.assertEqual(set(declared), {"good"})

    def test_whole_spec_of_wrong_shape(self):
        with self.assertLogs("plugin-manager", level="WARNING"):
            self.assertEqual(self.reg.declare("p", ["not", "a", "dict"]), {})
        self.assertEqual(self.reg.declare("p", {}), {})

    def test_invalid_values_are_dropped(self):
        declared = self.reg.declare("p", {"c": {"type": "counter", "labels": {"k": ["a"]}}, "g": {"type": "gauge"}})
        ops = [("inc", "c", -1, {"k": "a"}), ("inc", "c", True, {"k": "a"}), ("inc", "c", float("inf"), {"k": "a"}),
               ("inc", "c", 1, {}), ("inc", "c", 1, {"k": "a", "extra": "x"}),
               ("inc", "g", 1, {}), ("set", "c", 1, {"k": "a"}), ("inc", "missing", 1, {}), "garbage"]
        with self.assertLogs("plugin-manager", level="WARNING"):
            self.reg.apply("p", declared, ops)
        self.assertEqual(self.value("llm_plugin_c_total", plugin="p", k="a"), 0)
        self.assertEqual(self.value("llm_plugin_g", plugin="p"), 0)

    def test_same_declaration_can_be_shared_but_a_conflicting_one_is_refused(self):
        self.reg.declare("a", {"shared": {"type": "counter"}})
        self.assertEqual(set(self.reg.declare("b", {"shared": {"type": "counter"}})), {"shared"})
        with self.assertLogs("plugin-manager", level="WARNING") as logs:
            self.assertEqual(self.reg.declare("c", {"shared": {"type": "gauge"}}), {})
        self.assertTrue(any("conflicts" in line for line in logs.output))

    def test_a_warning_is_logged_once(self):
        declared = self.reg.declare("p", {"c": {"type": "counter"}})
        with self.assertLogs("plugin-manager", level="WARNING") as logs:
            for _ in range(5):
                self.reg.apply("p", declared, [("inc", "ghost", 1, {})])
        self.assertEqual(len(logs.output), 1)


if __name__ == "__main__":
    unittest.main()
