// The host follows the address the console is opened on; override it completely with VITE_GRAFANA_URL.
const DEFAULT = location.protocol + '//' + location.hostname + ':3000/d/llm-gateway-plugins/llm-gateway-c2b7-plugins?from=now-6h&to=now&timezone=browser&var-plugin=$__all&refresh=30s'

/** Link to the plugins dashboard in Grafana, optionally filtered to one plugin. */
export const grafanaUrl = (plugin?: string): string => {
  const base: string = import.meta.env.VITE_GRAFANA_URL || DEFAULT
  return plugin ? base.replace('var-plugin=$__all', 'var-plugin=' + encodeURIComponent(plugin)) : base
}
