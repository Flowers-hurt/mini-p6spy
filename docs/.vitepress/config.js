import { defineConfig } from 'vitepress';

export default defineConfig({
  lang: 'zh-CN',
  title: '手搓 mini p6spy 教程',
  description: '从 0 构建一个精简版 p6spy 的完整实践',
  base: './', // 使用相对路径，支持直接打开 HTML 文件
  ignoreDeadLinks: true,
  cleanUrls: false, // 禁用 clean URLs，使用 .html 后缀
  themeConfig: {
    nav: [
      { text: '首页', link: './' },
    ],
    sidebar: [
      {
        text: '教程',
        items: [
          { text: '从 0 开始', link: './' },
        ],
      },
    ],
    outline: [2, 3]
  }
});

