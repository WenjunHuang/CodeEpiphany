/* --------------------------------------------------------------------------------------------
 * Copyright (c) 2024 TypeFox and others.
 * Licensed under the MIT License. See LICENSE in the package root for license information.
 * ------------------------------------------------------------------------------------------ */
/// <reference types="vitest" />
import {defineConfig} from 'vite';
import importMetaUrlPlugin from '@codingame/esbuild-import-meta-url-plugin';
import {dirname, resolve} from 'node:path'
import {fileURLToPath} from "node:url";
import {intellijStylePrePlugin,intellijStylePostPlugin} from './plugin/intellijStylePlugin';

const __dirname = dirname(fileURLToPath(import.meta.url))

export const definedViteConfig = defineConfig({
    build: {
        target: 'ES2022',
        outDir: "../target/webviewResources/webview",
        rollupOptions: {
            input: {
                challengeDescription: resolve(__dirname, 'challengeDescription/index.html'),
                leetCodeSolutionArticle: resolve(__dirname, 'leetCodeSolutionArticle/index.html'),
            }
        },
        assetsDir: 'assets',
    },
    server: {
        port: 5173,
        host: true,
        cors: {
            origin: '*'
        },
        headers: {
            'Cross-Origin-Opener-Policy': 'same-origin',
            'Cross-Origin-Embedder-Policy': 'require-corp',
        },
        watch: {
            ignored: [
                '**/.chrome/**/*'
            ]
        }
    },
    optimizeDeps: {
        esbuildOptions: {
            plugins: [
                importMetaUrlPlugin
            ]
        },
        include: []
    },
    plugins: [
        intellijStylePrePlugin(),
        intellijStylePostPlugin()
    ],
    define: {
        rootDirectory: JSON.stringify(__dirname),
    },
    worker: {
        format: 'es'
    },
    esbuild: {
        minifySyntax: false
    }
});

export default definedViteConfig;
