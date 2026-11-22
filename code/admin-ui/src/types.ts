export type PluginType = 'request_filter' | 'response_filter'
export interface Plugin { name: string; type: PluginType; enabled: boolean; options: Record<string, unknown>; position?: number; description?: string; options_schema?: Record<string, unknown> }
export interface Status { enabled?: boolean; before?: string; after?: string }

export interface Page<T> { items: T[]; page: number; size: number; total: number }
export interface BlockInfo { reason?: string; blocked_at?: string; blocked_by?: string; keycloak_disabled?: boolean }
export interface UserView { id: string; username?: string; email?: string; enabled?: boolean; blocked?: boolean; block?: BlockInfo | null; last_activity?: string | null }
export interface UsageSummary { requests: number; errors: number; error_rate: number; prompt_tokens: number; completion_tokens: number; total_tokens: number; cost: number; currency?: string; avg_latency_ms: number; by_status?: Record<string, number> }
export interface Point { bucket: string; group?: string | null; requests: number; errors: number; prompt_tokens: number; completion_tokens: number; total_tokens: number; cost: number }
export interface Timeseries { interval: string; group_by?: string | null; points: Point[] }
export interface UsageEvent { timestamp: string; request_id: string; user_id: string; model: string; prompt_tokens: number; completion_tokens: number; total_tokens: number; latency_ms: number; status: string; error_code?: string | null; cost: number; currency?: string }
export interface ModelPricing { input_price_per_1m: number; output_price_per_1m: number; currency?: string }
export interface AdminModel { id: string; created?: number; owned_by?: string; pricing?: ModelPricing | null }
export interface PriceItem { model: string; input_price_per_1m: number; output_price_per_1m: number; currency: string; updated_at?: string }
export interface PricingTable { currency?: string; items: PriceItem[] }
export interface AuditEntry { timestamp: string; actor: string; actor_username?: string; action: string; target_type?: string; target_id?: string; before?: unknown; after?: unknown; request_id?: string }
