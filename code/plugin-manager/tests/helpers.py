import textwrap
from pathlib import Path
from tempfile import TemporaryDirectory

from prometheus_client import REGISTRY

from app.schemas import PluginSpec

REQUEST = {"model": "demo", "messages": [{"role": "user", "content": "hello"}]}
RESPONSE = {"choices": [{"index": 0, "message": {"role": "assistant", "content": "hi"}}]}


class PluginDir:
    """Временный PLUGINS_DIR с плагинами из строк."""

    def __init__(self):
        self._tmp = TemporaryDirectory()
        self.path = Path(self._tmp.name)

    def write(self, name: str, source: str) -> None:
        (self.path / f"{name}.py").write_text(textwrap.dedent(source))

    def cleanup(self):
        self._tmp.cleanup()


def spec(name: str, kind: str = "request_filter", **options) -> PluginSpec:
    return PluginSpec(name=name, type=kind, enabled=True, options=options)


def sample(name: str, **labels) -> float:
    return REGISTRY.get_sample_value(name, labels) or 0.0


REQUEST_FILTER = """
    from plugin_api import RequestFilter
    {imports}
    class P(RequestFilter):
        def apply(self, request):
    {body}
"""


def request_plugin(body: str, imports: str = "") -> str:
    """Исходник плагина запроса; body — тело apply с отступом в 4 пробела относительно def."""
    indented = textwrap.indent(textwrap.dedent(body), " " * 8)
    return f"from plugin_api import RequestFilter\n{imports}\n\nclass P(RequestFilter):\n    def apply(self, request):\n{indented}\n"
