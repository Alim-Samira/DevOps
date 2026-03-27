import { createRouter, createWebHistory } from 'vue-router'
import WatchPartyList from '../views/WatchPartyList.vue'
import WatchPartyDetails from '../views/WatchPartyDetails.vue'

const routes = [
  {
    path: '/',
    component: WatchPartyList
  },
  {
    path: '/watchparty/:id',
    component: WatchPartyDetails
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router