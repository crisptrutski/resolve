(ns bss.resolve.health)

;; on schedule:
;; 1. pull all services from registry
;; 2. mark all healthy services as pending-health
;; 3. start health checks (GET host:port/health)
;; 4. mark services that pass as healthy
;; 5. mark services that fail as unhealthy (no longer resolve, but keep
;; retrying)
;; 6. unregister services that have been unhealthy for too long
