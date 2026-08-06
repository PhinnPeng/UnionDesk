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
