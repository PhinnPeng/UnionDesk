/// <reference types="vitest/config" />

import process from "node:process";
import { fileURLToPath } from "node:url";
import { cleanupSVG, isEmptyColor, parseColors, runSVGO, SVG } from "@iconify/tools";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { codeInspectorPlugin } from "code-inspector-plugin";
import dayjs from "dayjs";
import { FileSystemIconLoader } from "unplugin-icons/loaders";
import Icons from "unplugin-icons/vite";
import { defineConfig } from "vite";
import { checker } from "vite-plugin-checker";
import { vitePluginFakeServer } from "vite-plugin-fake-server";
import svgrPlugin from "vite-plugin-svgr";

import { author, dependencies, devDependencies, license, name, version } from "./package.json";

function resolveDesignableAlias(subpath: string) {
	return fileURLToPath(new URL(`./node_modules/@designable/${subpath}`, import.meta.url));
}

const __APP_INFO__ = {
	pkg: { dependencies, devDependencies, name, version, license, author },
	lastBuildTime: dayjs(new Date()).format("YYYY-MM-DD HH:mm:ss"),
};

const isDev = process.env.NODE_ENV === "development";

// https://vitejs.dev/config/
export default defineConfig({

	base: isDev ? "/" : "/react-antd-admin/",
	resolve: {
		dedupe: ["react", "react-dom", "@formily/reactive", "@formily/reactive-react"],
		alias: {
			"@uniondesk/shared": fileURLToPath(new URL("../../packages/shared/src/index.ts", import.meta.url)),
			// 官方 @designable/* 包内部仍引用 @antd5-designable/*，统一指向同一构建
			"@antd5-designable/designable-core": resolveDesignableAlias("core"),
			"@antd5-designable/designable-react": resolveDesignableAlias("react"),
			"@antd5-designable/designable-formily-antd": resolveDesignableAlias("formily-antd"),
			"@antd5-designable/designable-formily-transformer": resolveDesignableAlias("formily-transformer"),
			"@antd5-designable/designable-react-settings-form": resolveDesignableAlias("react-settings-form"),
			"@antd5-designable/designable-formily-setters": resolveDesignableAlias("formily-setters"),
			"@antd5-designable/designable-shared": resolveDesignableAlias("shared"),
			// 强制 Designable 与主应用共用同一 React 实例（使用包目录以保留子路径解析）
			react: fileURLToPath(new URL("./node_modules/react", import.meta.url)),
			"react-dom": fileURLToPath(new URL("./node_modules/react-dom", import.meta.url)),
		},
	},
	optimizeDeps: {
		include: [
			"react",
			"react-dom",
			"@designable/core",
			"@designable/react",
			"@designable/formily-antd",
			"@designable/formily-transformer",
			"@designable/react-settings-form",
		],
	},
	plugins: [
		vitePluginFakeServer({
			basename: "/api",
			// 开发环境走真实后端（requestBackendJson 直连 8080）；避免 mock 与代理干扰排查
			enableProd: !isDev,
			timeout: 1000,
		}),
		// https://github.com/pd4d10/vite-plugin-svgr#options
		svgrPlugin({
			// https://react-svgr.com/docs/options/
			svgrOptions: {
				plugins: ["@svgr/plugin-svgo", "@svgr/plugin-jsx"],
				svgoConfig: {
					floatPrecision: 2,
				},
			},
		}),
		checker({
			typescript: true,
			terminal: false,
			enableBuild: false,
		}),
		/**
		 * 点击页面 DOM 打开 IDE 并将光标自动定位到源代码位置
		 *
		 * macOS 默认组合键 Option + Shift
		 * Windows 默认组合键 Alt + Shift
		 * 在 Web 页面上按住组合键时，移动鼠标即会在 DOM 上出现遮罩层并显示相关信息，鼠标点击一下，将自动打开 IDE 并将光标定位到元素对应的代码位置
		 * 更多用法看 https://inspector.fe-dev.cn/guide/start.html
		 */
		codeInspectorPlugin({
			bundler: "vite",
			// hideConsole: true,
		}),

		/**
		 * 按需加载图标
		 * https://github.com/antfu/unplugin-icons
		 */
		Icons({
			customCollections: {
				svg: FileSystemIconLoader("./src/icons/svg"),
			},
			/**
			 * @see https://iconify.design/docs/articles/cleaning-up-icons/#parsing-one-monotone-icon
			 * Cleaning up icons
			 * Set default color to currentColor
			 * Set default width and height to 1em
			 */
			transform: (svg, collection) => {
				if (collection === "svg") {
					const svgObject = new SVG(svg);
					cleanupSVG(svgObject);
					runSVGO(svgObject);
					parseColors(svgObject, {
						defaultColor: "currentColor",
						callback: (attr, colorStr, color) => {
							if (!color) {
								// Color cannot be parsed!
								throw new Error(`Invalid color: "${colorStr}" in attribute ${attr}`);
							}

							if (isEmptyColor(color)) {
								// Color is empty: 'none' or 'transparent'. Return as is
								return color;
							}

							// If color is not empty, return it
							return color;
						},
					});
					return svgObject.toString({ height: "1em", width: "1em" }); ;
				}
				return svg;
			},
			compiler: "jsx",
			jsx: "react",
			scale: 1,
		}),

		tailwindcss(),
		react(),
	],
	test: {
		globals: true,
		environment: "happy-dom",
		setupFiles: ["./src/setupTests.ts"],
	},
	server: {
		port: 3333,
		// https://vitejs.dev/config/server-options#server-proxy
		proxy: {
			"/api/v1": {
				target: "http://localhost:8080",
				changeOrigin: true,
			},
		},
	},
	define: {
		__APP_INFO__: JSON.stringify(__APP_INFO__),
	},
	build: {
		outDir: "build",
		sourcemap: false,
		rollupOptions: {
			output: {
				manualChunks(id) {
					const normalizedId = id.replaceAll("\\", "/");
					if (!normalizedId.includes("/node_modules/")) {
						return undefined;
					}
					if (normalizedId.includes("/@designable/") || normalizedId.includes("/@antd5-designable/")) {
						return "designable";
					}
					if (normalizedId.includes("/antd/") || normalizedId.includes("/@ant-design/icons/")) {
						return "antd";
					}
					if (normalizedId.includes("/@faker-js/faker/")) {
						return "faker";
					}
					return undefined;
				},
			},
		},
	},
});
