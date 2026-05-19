/* eslint-disable import/no-mutable-exports */
import {
	message as antdMessage,
	Modal as antdModal,
	notification as antdNotification,
	App,
} from "antd";

export type StaticAntdMessage = ReturnType<typeof App.useApp>["message"];
export type StaticAntdNotification = ReturnType<typeof App.useApp>["notification"];
export type StaticAntdModal = Omit<Pick<typeof antdModal, "info" | "success" | "error" | "warning" | "confirm" | "warn">, "warn">;

let message: StaticAntdMessage = antdMessage;
let notification: StaticAntdNotification = antdNotification;
let modal: StaticAntdModal = antdModal;

/**
 * @see https://ant.design/components/app
 * @see https://ant.design/docs/blog/why-not-static
 */
export function StaticAntd() {
	const staticFunctions = App.useApp();

	/* Usage 1 */
	message = staticFunctions.message;
	notification = staticFunctions.notification;
	modal = staticFunctions.modal;

	/* Usage 2 */
	window.$message = message;
	window.$modal = modal;
	window.$notification = notification;

	return null;
}

export {
	message,
	modal,
	notification,
};
