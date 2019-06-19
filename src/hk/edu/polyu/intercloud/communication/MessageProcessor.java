package hk.edu.polyu.intercloud.communication;

import hk.edu.polyu.intercloud.common.Common;

public class MessageProcessor extends Thread {

	public MessageProcessor(String name) {
		super(name);
	}

	@Override
	public void run() {
		while (true) {
			try {
				if ((!Common.gatewayQ.isEmpty())
						&& Common.executor.getActiveCount() < Common.gatewayWorkload) {
					Common.executor
							.submit(new Dispatch(Common.gatewayQ.poll()));
				}
				if ((!Common.gatewayQ_light.isEmpty())
						&& Common.executor_light.getActiveCount() < Common.gatewayWorkload_light) {
					Common.executor_light.submit(new Dispatch(
							Common.gatewayQ_light.poll()));
				}
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
