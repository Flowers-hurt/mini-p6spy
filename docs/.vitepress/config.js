import { defineConfig } from 'vitepress';

export default defineConfig({
  lang: 'zh-CN',
  title: '手搓 mini p6spy 教程',
  description: '从 0 构建一个精简版 p6spy 的完整实践',
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
    ],
    sidebar: [
      {
        text: '教程',
        items: [
          { text: '从 0 开始', link: '/' },
        ],
      },
    ],
    outline: [2, 3]
  }
});

