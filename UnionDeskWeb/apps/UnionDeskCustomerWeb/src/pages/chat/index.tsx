import { Link } from "react-router-dom";

export default function ChatPage() {
	return (
		<section className="ud-glass ud-glass--lg ud-chat-soon">
			<div className="ud-chat-soon__icon" aria-hidden />
			<p className="ud-kicker">二期能力</p>
			<h1 className="ud-title" style={{ fontSize: 28 }}>在线咨询即将开放</h1>
			<p className="ud-subtitle" style={{ maxWidth: 420, margin: "8px auto 20px" }}>
				当前可提交工单并跟踪进度。咨询上线后，可在此发起实时会话，也可从会话转工单。
			</p>
			<Link to="/tickets/new" className="ud-btn ud-btn--primary">去提交工单</Link>
		</section>
	);
}
