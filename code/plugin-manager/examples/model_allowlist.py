from plugin_api import Forbidden, RequestFilter


class ModelAllowlist(RequestFilter):
    """Пропускает только разрешённые модели. options: {"allowed_models": ["gpt-x"]}"""

    def apply(self, request: dict) -> dict:
        allowed = self.options.get("allowed_models", [])
        if request["model"] not in allowed:
            raise Forbidden(f"model '{request['model']}' is not allowed")
        return request
