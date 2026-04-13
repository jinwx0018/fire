<template>
  <label class="toggle-switch">
    <input
      type="checkbox"
      class="toggle-input"
      :checked="modelValue"
      @change="$emit('update:modelValue', $event.target.checked)"
    />
    <span class="toggle-slider" />
  </label>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 26px;
  flex-shrink: 0;
}

.toggle-input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e1;
  transition: 0.25s ease;
  border-radius: 26px;
}

.toggle-slider::before {
  position: absolute;
  content: '';
  height: 20px;
  width: 20px;
  left: 3px;
  bottom: 3px;
  background-color: var(--client-surface);
  transition: 0.25s ease;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
}

.toggle-input:checked + .toggle-slider {
  background-color: var(--client-primary);
}

.toggle-input:checked + .toggle-slider::before {
  transform: translateX(24px);
}

.toggle-input:focus-visible + .toggle-slider {
  box-shadow: 0 0 0 2px var(--client-surface), 0 0 0 4px var(--client-primary);
}
</style>
