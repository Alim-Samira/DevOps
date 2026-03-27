<template>
  <div>
    <h2>Liste des WatchParties</h2>

   <ul>
  <li v-for="wp in watchParties" :key="wp.id">
   <router-link :to="'/watchparty/' + wp.id">
    {{ wp.name }}
    </router-link>
    — {{ wp.state }} — {{ wp.date }}

    <div v-if="isAdmin">
      <button @click="changeState(wp, 'IN_PROGRESS')">Start</button>
      <button @click="changeState(wp, 'PAUSED')"> Pause</button>
      <button @click="changeState(wp, 'FINISHED')">End</button>
    </div>
  </li>
</ul>
  </div>
</template>

<script>
import { fetchWatchParties } from "../api/watchPartyApi";

export default {
  name: "WatchPartyList",

  data() {
    return {
      isAdmin: true,
      watchParties: []
    };
  },

  async mounted() {
    this.watchParties = await fetchWatchParties();
  },

  methods: {
    changeState(wp, newState) {
      wp.state = newState;
    },

    launchMiniGame(wp) {
      alert("Mini-jeu lancé pour : " + wp.name);
    }
  }
};
</script>


