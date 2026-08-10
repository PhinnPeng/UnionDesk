type IconProps = { className?: string };

function iconClass(className?: string): string {
	return className ? `ud-icon ${className}` : "ud-icon";
}

export function IconHome({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1v-9.5Z" strokeLinejoin="round" />
		</svg>
	);
}

export function IconTicket({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<path d="M4 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4V8Z" />
			<path d="M12 7v10" strokeLinecap="round" strokeDasharray="2 3" />
		</svg>
	);
}

export function IconBell({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<path d="M6 17h12l-1.2-1.8a6.4 6.4 0 0 1-1-3.5V9.2a3.8 3.8 0 1 0-7.6 0v2.5c0 1.25-.36 2.47-1.03 3.5L6 17Z" strokeLinejoin="round" />
			<path d="M10 19a2 2 0 0 0 4 0" strokeLinecap="round" />
		</svg>
	);
}

export function IconUser({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<circle cx="12" cy="8" r="3.2" />
			<path d="M5.5 19.2c1.5-3 4-4.5 6.5-4.5s5 1.5 6.5 4.5" strokeLinecap="round" />
		</svg>
	);
}

export function IconChat({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<path d="M5 7.5A2.5 2.5 0 0 1 7.5 5h9A2.5 2.5 0 0 1 19 7.5v6A2.5 2.5 0 0 1 16.5 16H12l-3.5 3v-3H7.5A2.5 2.5 0 0 1 5 13.5v-6Z" strokeLinejoin="round" />
		</svg>
	);
}

export function IconSearch({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<circle cx="11" cy="11" r="6.5" />
			<path d="m16.5 16.5 3.5 3.5" strokeLinecap="round" />
		</svg>
	);
}

export function IconChevron({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<path d="m9 6 6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
		</svg>
	);
}

export function IconPlus({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<path d="M12 5v14M5 12h14" strokeLinecap="round" />
		</svg>
	);
}

export function IconBack({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
			<path d="M15 6 9 12l6 6" strokeLinecap="round" strokeLinejoin="round" />
		</svg>
	);
}

/* 实心图标（Figma 132:155 服务台首页导出，fill=currentColor 由 CSS 着色） */

export function IconNavHome({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" width="20" height="20" fill="currentColor" aria-hidden>
			<path d="M8.33333 16.6667V11.6667H11.6667V16.6667H15.8333V10H18.3333L10 2.5L1.66667 10H4.16667V16.6667H8.33333Z" />
		</svg>
	);
}

export function IconNavTicket({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" width="20" height="20" fill="currentColor" aria-hidden>
			<path d="M11.6667 1.66667H5C4.08333 1.66667 3.34167 2.41667 3.34167 3.33333L3.33333 16.6667C3.33333 17.5833 4.075 18.3333 4.99167 18.3333H15C15.9167 18.3333 16.6667 17.5833 16.6667 16.6667V6.66667L11.6667 1.66667ZM13.3333 15H6.66667V13.3333H13.3333V15ZM13.3333 11.6667H6.66667V10H13.3333V11.6667ZM10.8333 7.5V2.91667L15.4167 7.5H10.8333Z" />
		</svg>
	);
}

export function IconNavChat({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" width="20" height="20" fill="currentColor" aria-hidden>
			<path d="M16.6667 1.66667H3.33333C2.41667 1.66667 1.675 2.41667 1.675 3.33333L1.66667 18.3333L5 15H16.6667C17.5833 15 18.3333 14.25 18.3333 13.3333V3.33333C18.3333 2.41667 17.5833 1.66667 16.6667 1.66667Z" />
		</svg>
	);
}

export function IconNavBell({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" width="20" height="20" fill="currentColor" aria-hidden>
			<path d="M10 18.3333C10.9167 18.3333 11.6667 17.5833 11.6667 16.6667H8.33333C8.33333 17.5833 9.075 18.3333 10 18.3333ZM15 13.3333V9.16667C15 6.60833 13.6333 4.46667 11.25 3.9V3.33333C11.25 2.64167 10.6917 2.08333 10 2.08333C9.30833 2.08333 8.75 2.64167 8.75 3.33333V3.9C6.35833 4.46667 5 6.6 5 9.16667V13.3333L3.33333 15V15.8333H16.6667V15L15 13.3333Z" />
		</svg>
	);
}

export function IconNavUser({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" width="20" height="20" fill="currentColor" aria-hidden>
			<path d="M10 10C11.8417 10 13.3333 8.50833 13.3333 6.66667C13.3333 4.825 11.8417 3.33333 10 3.33333C8.15833 3.33333 6.66667 4.825 6.66667 6.66667C6.66667 8.50833 8.15833 10 10 10ZM10 11.6667C7.775 11.6667 3.33333 12.7833 3.33333 15V16.6667H16.6667V15C16.6667 12.7833 12.225 11.6667 10 11.6667Z" />
		</svg>
	);
}

export function IconNavGear({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 14 14" width="14" height="14" fill="currentColor" aria-hidden>
			<path d="M11.165 7.54833C11.1883 7.37333 11.2 7.1925 11.2 7C11.2 6.81333 11.1883 6.62667 11.1592 6.45167L12.3433 5.53C12.4483 5.44833 12.4775 5.29083 12.4133 5.17417L11.2933 3.2375C11.2233 3.10917 11.0775 3.06833 10.9492 3.10917L9.555 3.66917C9.26333 3.4475 8.95417 3.26083 8.61 3.12083L8.4 1.63917C8.37667 1.49917 8.26 1.4 8.12 1.4H5.88C5.74 1.4 5.62917 1.49917 5.60583 1.63917L5.39583 3.12083C5.05167 3.26083 4.73667 3.45333 4.45083 3.66917L3.05667 3.10917C2.92833 3.0625 2.7825 3.10917 2.7125 3.2375L1.59833 5.17417C1.52833 5.29667 1.55167 5.44833 1.66833 5.53L2.8525 6.45167C2.82333 6.62667 2.8 6.81917 2.8 7C2.8 7.18083 2.81167 7.37333 2.84083 7.54833L1.65667 8.47C1.55167 8.55167 1.5225 8.70917 1.58667 8.82583L2.70667 10.7625C2.77667 10.8908 2.9225 10.9317 3.05083 10.8908L4.445 10.3308C4.73667 10.5525 5.04583 10.7392 5.39 10.8792L5.6 12.3608C5.62917 12.5008 5.74 12.6 5.88 12.6H8.12C8.26 12.6 8.37667 12.5008 8.39417 12.3608L8.60417 10.8792C8.94833 10.7392 9.26333 10.5525 9.54917 10.3308L10.9433 10.8908C11.0717 10.9375 11.2175 10.8908 11.2875 10.7625L12.4075 8.82583C12.4775 8.6975 12.4483 8.55167 12.3375 8.47L11.165 7.54833ZM7 9.1C5.845 9.1 4.9 8.155 4.9 7C4.9 5.845 5.845 4.9 7 4.9C8.155 4.9 9.1 5.845 9.1 7C9.1 8.155 8.155 9.1 7 9.1Z" />
		</svg>
	);
}

export function IconNavMessage({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 14 14" width="14" height="14" fill="currentColor" aria-hidden>
			<path d="M11.6667 1.16667H2.33333C1.69167 1.16667 1.16667 1.69167 1.16667 2.33333V12.8333L3.5 10.5H11.6667C12.3083 10.5 12.8333 9.975 12.8333 9.33333V2.33333C12.8333 1.69167 12.3083 1.16667 11.6667 1.16667ZM8.16667 7H5.83333V6.41667H8.16667V7ZM9.91667 5.25H4.08333V4.66667H9.91667V5.25ZM9.91667 3.5H4.08333V2.91667H9.91667V3.5Z" />
		</svg>
	);
}

export function IconNavDomain({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 14 14" width="14" height="14" fill="currentColor" aria-hidden>
			<path d="M7 4.08333V1.75H1.16667V12.25H12.8333V4.08333H7ZM3.5 11.0833H2.33333V9.91667H3.5V11.0833ZM3.5 8.75H2.33333V7.58333H3.5V8.75ZM3.5 6.41667H2.33333V5.25H3.5V6.41667ZM3.5 4.08333H2.33333V2.91667H3.5V4.08333ZM5.83333 11.0833H4.66667V9.91667H5.83333V11.0833ZM5.83333 8.75H4.66667V7.58333H5.83333V8.75ZM5.83333 6.41667H4.66667V5.25H5.83333V6.41667ZM5.83333 4.08333H4.66667V2.91667H5.83333V4.08333ZM11.6667 11.0833H7V9.91667H8.16667V8.75H7V7.58333H8.16667V6.41667H7V5.25H11.6667V11.0833ZM10.5 6.41667H9.33333V7.58333H10.5V6.41667ZM10.5 8.75H9.33333V9.91667H10.5V8.75Z" />
		</svg>
	);
}

export function IconSearchSolid({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 18 18" width="18" height="18" fill="currentColor" aria-hidden>
			<path d="M11.625 10.5H11.0325L10.8225 10.2975C11.5575 9.4425 12 8.3325 12 7.125C12 4.4325 9.8175 2.25 7.125 2.25C4.4325 2.25 2.25 4.4325 2.25 7.125C2.25 9.8175 4.4325 12 7.125 12C8.3325 12 9.4425 11.5575 10.2975 10.8225L10.5 11.0325V11.625L14.25 15.3675L15.3675 14.25L11.625 10.5ZM7.125 10.5C5.2575 10.5 3.75 8.9925 3.75 7.125C3.75 5.2575 5.2575 3.75 7.125 3.75C8.9925 3.75 10.5 5.2575 10.5 7.125C10.5 8.9925 8.9925 10.5 7.125 10.5Z" />
		</svg>
	);
}

export function IconStatDoc({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="24" height="24" fill="currentColor" aria-hidden>
			<path d="M14 2H6C4.9 2 4.01 2.9 4.01 4L4 20C4 21.1 4.89 22 5.99 22H18C19.1 22 20 21.1 20 20V8L14 2ZM16 18H8V16H16V18ZM16 14H8V12H16V14ZM13 9V3.5L18.5 9H13Z" />
		</svg>
	);
}

export function IconStatClock({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="24" height="24" fill="currentColor" aria-hidden>
			<path d="M11.99 2C6.47 2 2 6.48 2 12C2 17.52 6.47 22 11.99 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 11.99 2ZM12 20C7.58 20 4 16.42 4 12C4 7.58 7.58 4 12 4C16.42 4 20 7.58 20 12C20 16.42 16.42 20 12 20ZM12.5 7H11V13L16.25 16.15L17 14.92L12.5 12.25V7Z" />
		</svg>
	);
}

export function IconStatCheck({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 24 24" width="24" height="24" fill="currentColor" aria-hidden>
			<path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM10 17L5 12L6.41 10.59L10 14.17L17.59 6.58L19 8L10 17Z" />
		</svg>
	);
}

export function IconNotifTicket({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" width="20" height="20" fill="currentColor" aria-hidden>
			<path d="M11.6667 1.66667H5C4.55797 1.66667 4.13405 1.84226 3.82149 2.15482C3.50893 2.46738 3.33333 2.89131 3.33333 3.33333V16.6667C3.33333 17.1087 3.50893 17.5326 3.82149 17.8452C4.13405 18.1577 4.55797 18.3333 5 18.3333H15C15.442 18.3333 15.8659 18.1577 16.1785 17.8452C16.4911 17.5326 16.6667 17.1087 16.6667 16.6667V6.66667L11.6667 1.66667Z" />
		</svg>
	);
}

export function IconNotifShield({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" width="20" height="20" fill="currentColor" aria-hidden>
			<path d="M10 18.3333C10 18.3333 16.6667 15 16.6667 10V4.16667L10 1.66667L3.33333 4.16667V10C3.33333 15 10 18.3333 10 18.3333Z" />
		</svg>
	);
}
