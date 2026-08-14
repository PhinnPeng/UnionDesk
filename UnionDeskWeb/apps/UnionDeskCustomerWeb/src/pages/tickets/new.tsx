import {
	createCustomerTicketLive,
	refreshCustomerTicketTypesLive,
	useCustomerPortal,
} from "@uniondesk/shared";
import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { useToast } from "../../components/Toast";

type Step = "type" | "form" | "done";

export default function TicketCreatePage() {
	const portal = useCustomerPortal();
	const toast = useToast();
	const navigate = useNavigate();
	const [step, setStep] = useState<Step>("type");
	const [typeId, setTypeId] = useState("");
	const [title, setTitle] = useState("");
	const [description, setDescription] = useState("");
	const [loading, setLoading] = useState(false);
	const [createdNo, setCreatedNo] = useState<string | null>(null);
	const [createdId, setCreatedId] = useState<string | null>(null);

	useEffect(() => {
		void refreshCustomerTicketTypesLive().catch(() => undefined);
	}, [portal.activeDomain?.id]);

	const selectedType = portal.ticketTypes.find(item => item.id === typeId);

	const chooseType = (id: string) => {
		setTypeId(id);
		setStep("form");
	};

	const handleSubmit = async (event: FormEvent) => {
		event.preventDefault();
		if (!portal.activeDomain) {
			toast.error("请先选择业务域");
			navigate("/domains");
			return;
		}
		if (!typeId) {
			toast.error("请先选择工单类型");
			setStep("type");
			return;
		}
		setLoading(true);
		try {
			const created = await createCustomerTicketLive({
				typeId,
				title: title.trim(),
				description: description.trim(),
				attachmentIds: [],
			});
			toast.success("工单已提交");
			setCreatedNo(created.ticketNo);
			setCreatedId(created.ticketId);
			setStep("done");
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "提交失败");
		}
		finally {
			setLoading(false);
		}
	};

	if (step === "done" && createdNo && createdId != null) {
		return (
			<section className="ud-glass ud-glass--lg" style={{ padding: 28, textAlign: "center" }}>
				<p className="ud-kicker">提交成功</p>
				<h1 className="ud-title" style={{ fontSize: 34 }}>我们已收到</h1>
				<p className="ud-subtitle">工单号</p>
				<p className="ud-mono" style={{ fontSize: 22, fontWeight: 700, margin: "8px 0 20px" }}>{createdNo}</p>
				<div className="ud-row" style={{ justifyContent: "center" }}>
					<button type="button" className="ud-btn ud-btn--primary" onClick={() => navigate(`/tickets/${createdId}`)}>
						查看详情
					</button>
					<button type="button" className="ud-btn ud-btn--ghost" onClick={() => navigate("/tickets")}>
						返回列表
					</button>
				</div>
			</section>
		);
	}

	if (step === "type") {
		return (
			<div className="ud-stack ud-stack--lg">
				<header>
					<div className="ud-steps">
						<span className="is-active">1. 选择类型</span>
						<span>2. 填写详情</span>
						<span>3. 完成</span>
					</div>
					<p className="ud-kicker">新请求</p>
					<h1 className="ud-title" style={{ fontSize: 28 }}>选择工单类型</h1>
					<p className="ud-subtitle">请选择当前业务域已启用的类型，再填写详情。</p>
				</header>
				{portal.ticketTypes.length === 0
					? <div className="ud-glass ud-empty">暂无可用工单类型，请联系管理员配置。</div>
					: (
						<div className="ud-type-grid">
							{portal.ticketTypes.map(type => (
								<button
									key={type.id}
									type="button"
									className="ud-type-card ud-glass"
									onClick={() => chooseType(type.id)}
								>
									<h3>{type.name}</h3>
									<p>{type.description_template_md?.trim() || "点击继续填写详情"}</p>
								</button>
							))}
						</div>
					)}
			</div>
		);
	}

	return (
		<div className="ud-stack ud-stack--lg">
			<header>
				<div className="ud-steps">
					<span>1. 选择类型</span>
					<span className="is-active">2. 填写详情</span>
					<span>3. 完成</span>
				</div>
				<p className="ud-kicker">新请求</p>
				<h1 className="ud-title" style={{ fontSize: 28 }}>填写详情</h1>
				<p className="ud-subtitle">
					类型：
					<strong>{selectedType?.name ?? "未选择"}</strong>
				</p>
			</header>

			<form className="ud-glass ud-glass--lg ud-form" style={{ padding: 22 }} onSubmit={handleSubmit}>
				<div className="ud-row ud-row--between">
					<span className="ud-tag ud-tag--blue">{selectedType?.name}</span>
					<button type="button" className="ud-btn ud-btn--ghost ud-btn--sm" onClick={() => setStep("type")}>
						更改类型
					</button>
				</div>
				<div className="ud-field">
					<label htmlFor="title">标题</label>
					<input
						id="title"
						className="ud-input"
						value={title}
						onChange={event => setTitle(event.target.value)}
						placeholder="一句话概括问题"
						required
					/>
				</div>
				<div className="ud-field">
					<label htmlFor="description">详细说明</label>
					<textarea
						id="description"
						className="ud-textarea"
						value={description}
						onChange={event => setDescription(event.target.value)}
						placeholder="现象、复现步骤、期望结果…"
						required
					/>
				</div>
				<div className="ud-row">
					<button className="ud-btn ud-btn--primary" type="submit" disabled={loading}>
						{loading ? "提交中…" : "提交"}
					</button>
					<button className="ud-btn ud-btn--ghost" type="button" onClick={() => navigate(-1)}>
						取消
					</button>
				</div>
			</form>
		</div>
	);
}
