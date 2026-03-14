<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand-block">
        <div class="brand-mark">
          <img :src="logo" alt="青青课堂 logo" />
        </div>
        <div>
          <strong>青青课堂</strong>
          <p>运营后台</p>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        class="side-menu"
        router
      >
        <el-menu-item
          v-for="item in navigationItems"
          :key="item.path"
          :index="item.path"
          @click="mobileNavOpen = false"
        >
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <div class="side-menu__meta">
            <span>{{ item.meta.label }}</span>
            <small>{{ item.meta.caption }}</small>
          </div>
        </el-menu-item>
      </el-menu>

    </aside>

    <el-drawer v-model="mobileNavOpen" direction="ltr" size="260px" :with-header="false">
      <div class="brand-block brand-block--drawer">
        <div class="brand-mark">
          <img :src="logo" alt="青青课堂 logo" />
        </div>
        <div>
          <strong>青青课堂</strong>
          <p>运营后台</p>
        </div>
      </div>
      <el-menu :default-active="route.path" class="side-menu side-menu--drawer" router>
        <el-menu-item
          v-for="item in navigationItems"
          :key="item.path"
          :index="item.path"
          @click="mobileNavOpen = false"
        >
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <div class="side-menu__meta">
            <span>{{ item.meta.label }}</span>
            <small>{{ item.meta.caption }}</small>
          </div>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <div class="admin-workspace">
      <header class="workspace-header">
        <div class="workspace-header__left">
          <el-button class="workspace-header__menu" circle :icon="Menu" @click="mobileNavOpen = true" />
          <div>
            <span class="workspace-header__eyebrow">Qingqing Ketang</span>
            <strong>{{ route.meta.title }}</strong>
          </div>
        </div>
        <el-tag effect="plain" round class="workspace-header__date">
          {{ todayLabel }}
        </el-tag>
      </header>

      <main class="workspace-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import { Menu } from '@element-plus/icons-vue';
import { navigationItems } from '../constants/navigation';
import { formatLongDate } from '../utils/format';
import logo from '../../img/logo.png';

const route = useRoute();
const mobileNavOpen = ref(false);
const todayLabel = computed(() => formatLongDate(new Date()));
</script>
