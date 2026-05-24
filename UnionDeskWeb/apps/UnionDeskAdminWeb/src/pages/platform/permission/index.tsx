import { Navigate } from "react-router";

/** 权限管理为分组菜单，无独立页面，进入后跳转至角色管理 */
export default function PlatformPermissionRedirect() {
	return <Navigate to="/platform/role" replace />;
}
