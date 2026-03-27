export function fetchWatchParties() {
  return Promise.resolve([
    {
      id: 1,
      name: "Finale Worlds",
      date: "2025-12-25",
      state: "PAUSED"
    },
    {
      id: 2,
      name: "LEC Spring",
      date: "2025-12-30",
      state: "IN_PROGRESS"
    }
  ]);
}
