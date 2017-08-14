package prj.jolokiaweb.task;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pRequest;
import org.jolokia.client.request.J4pResponse;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import prj.jolokiaweb.JolokiaApp;
import prj.jolokiaweb.jolokia.JolokiaClient;
import prj.jolokiaweb.websocket.DashboardHandler;
import prj.jolokiaweb.websocket.Message;

import javax.management.MalformedObjectNameException;
import java.util.List;

@Component
public class DashboardUpdate {

    @Autowired
    private JolokiaClient jolokiaClient;

    @Autowired
    private DashboardHandler handler;

    @Scheduled(fixedDelay=3000)
    public void publishUpdates(){
        if (handler.getClientNum() < 1) {
            return;
        }
        JSONObject result = new JSONObject();
        try {
            J4pReadRequest osReq = new J4pReadRequest("java.lang:type=OperatingSystem",
                    "ProcessCpuLoad",
                    "SystemCpuLoad",
                    "TotalSwapSpaceSize",
                    "FreeSwapSpaceSize",
                    "TotalPhysicalMemorySize",
                    "FreePhysicalMemorySize"
            );
            J4pReadRequest threadReq = new J4pReadRequest("java.lang:type=Threading","ThreadCount","PeakThreadCount");
            J4pReadRequest memoryHeapReq = new J4pReadRequest("java.lang:type=Memory","HeapMemoryUsage", "NonHeapMemoryUsage");
            List<J4pResponse<J4pRequest>> responseList = jolokiaClient.getClient().execute(osReq, threadReq, memoryHeapReq);

            result.put("os", responseList.get(0).getValue());
            result.put("thread", responseList.get(1).getValue());
            result.put("memory", responseList.get(2).getValue());

            handler.sendDashboardStats(new Message("dashboard", result));
        } catch (MalformedObjectNameException e) {
            System.err.println(e.getMessage());
        } catch (J4pException e) {
            System.err.println(e.getMessage());
        }
    }
}
