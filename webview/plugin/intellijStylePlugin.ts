import type {Plugin} from 'vite';

const excludes = ['luoguYiDun/index.html'];

/**
 * Vite 插件：自动注入样式表链接和重载功能
 */
export function intellijStylePostPlugin(): Plugin {
    return {
        name: 'intellij-style-post-plugin',
        transformIndexHtml: {
            order: 'post',
            handler: (html, ctx) => {
                if (excludes.some(exclude => ctx.path.endsWith(exclude))) {
                    return html;
                }
                return {
                    html,
                    tags: [
                        {
                            tag: 'script',
                            attrs: {
                                type: 'text/javascript'
                            },
                            // language=JavaScript
                            children: `
                                function onIntellijStyleLoaded() {
                                    // 检查是否启用暗黑模式
                                    console.log('Intellij style loaded');
                                    const isDarkMode = getComputedStyle(document.documentElement)
                                        .getPropertyValue('--darkMode')
                                        .trim() === '1';
                                    document.documentElement.setAttribute('data-theme', isDarkMode ? 'dark' : 'light');
                                }
                            `
                        },
                        {
                            tag: 'link',
                            attrs: {
                                id: 'intellijStyle',
                                rel: 'stylesheet',
                                type: 'text/css',
                                href: '/intellijStyle.css',
                                onload: 'onIntellijStyleLoaded()'
                            }
                        }
                    ]
                }
            }
        }
    }
}

export function intellijStylePrePlugin(): Plugin {
    return {
        name: 'intellij-style-plugin',
        transformIndexHtml: {
            order: "pre",
            handler: (html, ctx) => {
                if (excludes.some(exclude => ctx.path.endsWith(exclude))) {
                    return html;
                }
                return {
                    html,
                    tags: [
                        {
                            tag: 'script',
                            attrs: {
                                type: 'module',
                            },
                            // language=JavaScript
                            children: `
                                import 'normalize-css/normalize.css';
                                import 'overlayscrollbars/overlayscrollbars.css';
                                import {OverlayScrollbars} from "overlayscrollbars";

                                // 缩放相关常量
                                const MAX_ZOOM = 500;
                                const MIN_ZOOM = 25;

                                // 样式重载函数
                                function reloadStyles() {
                                    const styleElement = document.getElementById('intellijStyle');
                                    if (styleElement && styleElement.tagName === 'LINK') {
                                        const newStyle = document.createElement('LINK');
                                        newStyle.id = 'intellijStyle';
                                        newStyle.rel = 'stylesheet';
                                        newStyle.type = 'text/css';
                                        newStyle.href = appendTimestamp(styleElement.href);
                                        newStyle.onload = () => {
                                            onIntellijStyleLoaded();
                                        };
                                        styleElement.parentNode?.replaceChild(newStyle, styleElement);
                                    }
                                }

                                // 为URL添加时间戳
                                function appendTimestamp(url) {
                                    const patchedUrl = new URL(url);
                                    patchedUrl.searchParams.set('timestamp', new Date().getTime().toString());
                                    return patchedUrl.toString();
                                }

                                // 初始化滚动条
                                OverlayScrollbars(document.body, {});

                                // 状态管理
                                const gState = {
                                    'zoom': 100
                                };

                                // 缩放相关函数
                                function setZoom(zoom = 100) {
                                    zoom = normalizeZoom(zoom);
                                    const container = document.getElementById("container");

                                    if (zoom === 100) {
                                        container.style.zoom = '';
                                    } else {
                                        container.style.zoom = \`\${zoom}%\`;
                                    }

                                    gState.zoom = zoom;
                                    _updateInfo();
                                }

                                function zoomIn() {
                                    setZoom(gState.zoom * 1.2);
                                }

                                function zoomOut() {
                                    setZoom(gState.zoom / 1.2);
                                }

                                function actualZoom() {
                                    setZoom(100);
                                }

                                function normalizeZoom(zoom) {
                                    return Math.min(Math.max(zoom, MIN_ZOOM), MAX_ZOOM);
                                }

                                // 初始化函数
                                function _init() {
                                    setZoom(gState.zoom);
                                    window.removeEventListener('load', _init);
                                }

                                window.addEventListener('load', _init);

                                // 更新状态信息
                                function _updateInfo() {
                                    const info = {
                                        'zoom': gState.zoom,
                                        'canZoomIn': gState.zoom < MAX_ZOOM,
                                        'canZoomOut': gState.zoom > MIN_ZOOM
                                    };
                                    window.sendInfo && window.sendInfo(JSON.stringify(info));
                                }

                                // 暴露全局函数
                                window.setZoom = setZoom;
                                window.zoomIn = zoomIn;
                                window.zoomOut = zoomOut;
                                window.actualZoom = actualZoom;
                                window.reloadStyles = reloadStyles;
                            `
                        }
                    ]
                };
            }
        }
    };
}

