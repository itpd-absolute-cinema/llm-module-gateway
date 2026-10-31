# Example usage

```bash
# 1. upload 5 plugins
python3 llm_gateway_cli.py plugins upload /Users/b4lmor/Work/inno/itpd/code/plugin-manager/examples/mask_phone.py --type request_filter
python3 llm_gateway_cli.py plugins upload /Users/b4lmor/Work/inno/itpd/code/plugin-manager/examples/mask_ip.py --type request_filter
python3 llm_gateway_cli.py plugins upload /Users/b4lmor/Work/inno/itpd/code/plugin-manager/examples/route_model.py --type request_filter
python3 llm_gateway_cli.py plugins upload /Users/b4lmor/Work/inno/itpd/code/plugin-manager/examples/limit_input_length.py --type request_filter
python3 llm_gateway_cli.py plugins upload /Users/b4lmor/Work/inno/itpd/code/plugin-manager/examples/mask_card.py --type request_filter

# 2. configure each
python3 llm_gateway_cli.py plugins update route_model --options '{"long_model":"gpt-x-long","threshold_chars":100}'
python3 llm_gateway_cli.py plugins update limit_input_length --options '{"max_chars":8000}'

# 3. enable each
python3 llm_gateway_cli.py plugins patch mask_card --enabled true
python3 llm_gateway_cli.py plugins patch mask_ip --enabled true
python3 llm_gateway_cli.py plugins patch mask_phone --enabled true
python3 llm_gateway_cli.py plugins patch route_model --enabled true
python3 llm_gateway_cli.py plugins patch limit_input_length --enabled true

# 4. arrange
python3 llm_gateway_cli.py plugins patch mask_card --before mask_ip
python3 llm_gateway_cli.py plugins patch mask_ip --before mask_phone

# 5. verify
python3 llm_gateway_cli.py plugins list
```
