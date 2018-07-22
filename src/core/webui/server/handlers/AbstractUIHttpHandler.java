package core.webui.server.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;

import core.ipc.IIPCService;
import core.ipc.IPCServiceManager;
import core.userDefinedTask.TaskGroup;
import core.webcommon.HttpServerUtilities;
import core.webui.server.handlers.renderedobjects.ObjectRenderer;
import core.webui.server.handlers.renderedobjects.RenderedIPCService;
import core.webui.server.handlers.renderedobjects.RenderedTaskGroup;
import core.webui.server.handlers.renderedobjects.RenderedUserDefinedAction;

public abstract class AbstractUIHttpHandler extends AbstractSingleMethodHttpHandler {

	private static final Logger LOGGER = Logger.getLogger(AbstractUIHttpHandler.class.getName());

	protected ObjectRenderer objectRenderer;

	public AbstractUIHttpHandler(ObjectRenderer objectRenderer, String allowedMethod) {
		super(allowedMethod);
		this.objectRenderer = objectRenderer;
	}

	protected final Void renderedIpcServices(HttpAsyncExchange exchange) throws IOException {
		Map<String, Object> data = new HashMap<>();
		List<RenderedIPCService> services = new ArrayList<>(IPCServiceManager.IPC_SERVICE_COUNT);
		for (int i = 0; i < IPCServiceManager.IPC_SERVICE_COUNT; i++) {
			IIPCService service = IPCServiceManager.getIPCService(i);
			services.add(RenderedIPCService.fromIPCService(service));
		}
		data.put("ipcs", services);

		return renderedPage(exchange, "rendered_ipcs", data);
	}

	protected final Void renderedTaskForGroup(HttpAsyncExchange exchange) throws IOException {
		Map<String, Object> data = new HashMap<>();
		TaskGroup group = backEndHolder.getCurrentTaskGroup();
		List<RenderedUserDefinedAction> taskList = group.getTasks().stream().map(RenderedUserDefinedAction::fromUserDefinedAction).collect(Collectors.toList());
		data.put("tasks", taskList);

		return renderedPage(exchange, "rendered_tasks", data);
	}

	protected final Void renderedTaskGroups(HttpAsyncExchange exchange) throws IOException {
		Map<String, Object> data = new HashMap<>();
		data.put("groups", backEndHolder.getTaskGroups()
			.stream().map(g -> RenderedTaskGroup.fromTaskGroup(g, g == backEndHolder.getCurrentTaskGroup()))
			.collect(Collectors.toList()));

		return renderedPage(exchange, "rendered_task_groups", data);
	}

	protected final Void renderedPage(HttpAsyncExchange exchange, String template, Map<String, Object> data) throws IOException {
		String page = objectRenderer.render(template, data);
		if (page == null) {
			return HttpServerUtilities.prepareHttpResponse(exchange, 500, "Failed to render page.");
		}

		return HttpServerUtilities.prepareHttpResponse(exchange, HttpStatus.SC_OK, page);
	}
}