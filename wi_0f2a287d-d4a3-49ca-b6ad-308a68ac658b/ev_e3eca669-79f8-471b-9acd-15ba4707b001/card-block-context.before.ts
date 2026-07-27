import type { ItemSummary } from '@workq/protocol';

/**
 * Why a `waiting_resume` card is parked for capacity — the wake reason plus
 * "waiting for a slot", matching the waiting-resume harness copy.
 */
export function resumeSlotLine(item: ItemSummary): string | undefined {
  if (item.status !== 'waiting_resume') return undefined;
  if (item.landing) return 'approved · waiting for a slot';
  if (item.revision) {
    const why =
      item.revision.reason === 'conflict'
        ? 'conflict'
        : item.revision.reason === 'ci'
          ? 'CI'
          : item.revision.reason === 'response'
            ? 'author replied'
            : 'revise';
    return `${why} · waiting for a slot`;
  }
  // Feedback / take / other claimable parks leave no landing or revision marker.
  return 'answer landed · waiting for a slot';
}

/** To do semantic buckets — always-on filters, hide empty. */
export type TodoSemanticFilter = 'untriaged' | 'deps' | 'human';

export const TODO_SEMANTIC_FILTERS: readonly {
  id: TodoSemanticFilter;
  /** Short pill copy — keep to one or two words so four filters fit one row. */
  label: string;
  /** Long bucket name for hover / title (“Show only …”). */
  fullLabel: string;
  match: (item: ItemSummary) => boolean;
}[] = [
  {
    id: 'untriaged',
    label: 'Triage',
    fullLabel: 'Untriaged',
    match: (item) => item.status === 'todo' && item.triage === undefined,
  },
  {
    id: 'deps',
    label: 'Deps',
    fullLabel: 'Waiting for dependencies',
    match: (item) => (item.dependencies?.blockedBy ?? 0) > 0,
  },
  {
    id: 'human',
    label: 'Human',
    fullLabel: 'Waiting for human',
    match: (item) => item.status === 'needs_human',
  },
];

/**
 * Exclusive All-lane bucket for a To do card. Pill `match` stays non-exclusive
 * (counts can overlap); All subsections put each card in exactly one group.
 * Priority: human > deps > untriaged. `null` = claimable residual (triaged,
 * unblocked todo) that matches no semantic filter.
 */
export const TODO_EXCLUSIVE_BUCKET_ORDER: readonly TodoSemanticFilter[] = [
  'human',
  'deps',
  'untriaged',
];

export function todoExclusiveBucket(item: ItemSummary): TodoSemanticFilter | null {
  for (const id of TODO_EXCLUSIVE_BUCKET_ORDER) {
    const filter = TODO_SEMANTIC_FILTERS.find((f) => f.id === id);
    if (filter?.match(item)) return id;
  }
  return null;
}

export function needsTriageLine(item: ItemSummary): boolean {
  return item.status === 'todo' && item.triage === undefined;
}

/** One-line ChainBadge copy when an item is blocked on unmet deps. */
export function blockedChainLabel(summary: NonNullable<ItemSummary['dependencies']>): string {
  const titles = summary.blockedTitles?.filter((t) => t.trim().length > 0) ?? [];
  if (titles.length === 1) return `Waiting on ${titles[0]}`;
  if (titles.length > 1) return `Waiting on ${titles[0]} +${titles.length - 1}`;
  const { waitingOn, blockedBy } = summary;
  return `Waiting on ${blockedBy < waitingOn ? `${blockedBy} of ${waitingOn}` : blockedBy}${
    blockedBy === waitingOn ? (blockedBy === 1 ? ' ticket' : ' tickets') : ''
  }`;
}
