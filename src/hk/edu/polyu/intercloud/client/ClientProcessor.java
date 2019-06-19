package hk.edu.polyu.intercloud.client;

import hk.edu.polyu.intercloud.common.Common;

public class ClientProcessor implements Runnable {

	@Override
	public void run() {
		while (true) {
			if ((!Common.clientQ.isEmpty())
					&& Common.clientExecutor.getActiveCount() < Common.clientWorkload) {
				Common.clientExecutor.submit(new ClientInvoker(Common.clientQ
						.poll()));
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
	}

}
