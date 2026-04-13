<template>
  <div
    class="km-root"
    :class="{
      'km-root--embed': variant === 'embed',
      'km-page': variant !== 'embed',
      'km-page--narrow': variant !== 'embed' && narrow,
    }"
  >
    <nav v-if="crumbs && crumbs.length" class="km-breadcrumb" aria-label="面包屑">
      <span v-for="(c, i) in crumbs" :key="'km-crumb-' + i" class="km-bc-wrap">
        <span v-if="i > 0" class="km-bc-sep" aria-hidden="true">/</span>
        <router-link v-if="c.to != null && c.to !== ''" :to="c.to" class="km-bc-link">{{ c.label }}</router-link>
        <span v-else class="km-bc-current" :title="c.titleAttr || (typeof c.label === 'string' ? c.label : '')">{{
          c.label
        }}</span>
      </span>
    </nav>
    <div v-if="variant !== 'embed'" class="km-panel">
      <slot />
    </div>
    <slot v-else />
  </div>
</template>

<script setup>
defineProps({
  crumbs: { type: Array, default: () => [] },
  /** 详情 / 表单等较窄版心 */
  narrow: { type: Boolean, default: false },
  /** 仅面包屑 + 默认插槽，外层卡片由父级提供（如知识列表主栏） */
  variant: { type: String, default: 'page' },
})
</script>

<style scoped>
.km-page .km-breadcrumb,
.km-root--embed .km-breadcrumb {
  margin-bottom: 14px;
}

.km-page {
  width: 100%;
  max-width: var(--client-content-max);
  margin: 0 auto;
  padding: 0 clamp(16px, 2.5vw, 40px) 28px;
  box-sizing: border-box;
}

.km-page--narrow {
  max-width: 960px;
}

.km-breadcrumb {
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.5;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 6px;
}

.km-bc-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
}

.km-bc-link {
  color: var(--client-primary);
  text-decoration: none;
}

.km-bc-link:hover {
  text-decoration: underline;
  color: var(--client-primary-hover);
}

.km-bc-sep {
  opacity: 0.55;
  user-select: none;
}

.km-bc-current {
  color: var(--client-text);
  font-weight: 500;
  max-width: min(100%, 480px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.km-panel {
  background: var(--client-surface);
  border-radius: var(--client-radius);
  box-shadow: var(--client-shadow);
  border: 1px solid rgba(59, 130, 246, 0.12);
  padding: 20px 22px 28px;
  min-width: 0;
}
</style>
