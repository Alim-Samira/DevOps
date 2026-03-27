<template>
  <div class="page" v-if="watchParty">
    <h2>Détails de la WatchParty</h2>

    <div class="card">
      <p><strong>Nom :</strong> {{ watchParty.name }}</p>
      <p><strong>Date :</strong> {{ watchParty.date }}</p>
      <p><strong>Type :</strong> {{ watchParty.type }}</p>
      <p><strong>Participants :</strong> {{ watchParty.participants }}</p>
      <p><strong>État :</strong> {{ watchParty.state }}</p>

      <div class="toolbar">
        <label>
          <input type="checkbox" v-model="isAdmin" />
          Mode admin
        </label>
      </div>

      <div v-if="isAdmin" class="actions">
        <button @click="changeState('PRE_MATCH')">Avant match</button>
        <button @click="changeState('IN_PROGRESS')">En cours</button>
        <button @click="changeState('PAUSED')">Pause</button>
        <button @click="changeState('FINISHED')">Terminé</button>
      </div>

      <button
        :disabled="watchParty.state !== 'PAUSED'"
        @click="launchMiniGame"
      >
        🎮 Lancer mini-jeu
      </button>
    </div>

    <div class="history">
      <h3>Historique des états</h3>
      <ul>
        <li v-for="(item, index) in history" :key="index">
          {{ item }}
        </li>
      </ul>
    </div>

    <router-link to="/">⬅ Retour à la liste</router-link>
  </div>
</template>

<script>
import { fetchWatchPartyById } from "../api/watchPartyApi";

export default {
  name: "WatchPartyDetails",

  data() {
    return {
      isAdmin: true,
      watchParty: null,
      history: []
    };
  },

  async mounted() {
    this.watchParty = await fetchWatchPartyById(this.$route.params.id);
    if (this.watchParty) {
      this.history.push(`État initial : ${this.watchParty.state}`);
    }
  },

  methods: {
    changeState(newState) {
      this.watchParty.state = newState;
      this.history.push(`État changé vers : ${newState}`);
    },

    launchMiniGame() {
      this.history.push("Mini-jeu lancé");
      alert("Mini-jeu lancé !");
    }
  }
};
</script>

<style scoped>
.page {
  padding: 20px;
  font-family: Arial, sans-serif;
}

.card {
  border: 1px solid #ddd;
  border-radius: 10px;
  padding: 16px;
  background: #fafafa;
  margin-bottom: 20px;
}

.actions {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.history {
  margin-bottom: 20px;
}
</style>