import { config } from './config'

const PATH = '/d/llm-gateway-plugins/llm-gateway-c2b7-plugins?from=now-6h&to=now&timezone=browser&var-plugin=$__all&refresh=30s'

/**
 * Link to the plugins dashboard in Grafana, optionally filtered to one plugin.
 * GRAFANA_PUBLIC_URL may be a Grafana address (the dashboard path is added) or a full dashboard URL.
 * Without it the host follows the address the console is opened on, port 3000.
 */
export const grafanaUrl = (plugin?: string): string => {
  const g = config.grafanaUrl
  const base = !g ? location.protocol + '//' + location.hostname + ':3000' + PATH : g.includes('/d/') ? g : g.replace(/\/+$/, '') + PATH
  return plugin ? base.replace('var-plugin=$__all', 'var-plugin=' + encodeURIComponent(plugin)) : base
}
