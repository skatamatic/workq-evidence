# Lens: concurrency
Findings: none medium+
Lock serializes install; second waiter reuses; eject-while-waiting fails closed without selection; dual-transport exclusivity preserved under same lock.
