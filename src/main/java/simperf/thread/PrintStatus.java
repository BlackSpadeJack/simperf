package simperf.thread;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import simperf.result.DataStatistics;
import simperf.util.SimperfUtil;

/**
 * ��ӡͳ���߳�
 * ALL: avgTps=ƽ��TPS ,count=�����ܼ��� ,duration=�����ܺ�ʱ ,fail=����ʧ����
 * NOW: tTps=��ǰʱ���TPS ,tCount=��ǰʱ��μ��� ,tDuration=��ǰʱ��κ�ʱ ,tFail=��ǰʱ���ʧ����
 * @author imbugs
 */
public class PrintStatus extends Thread {
    private SimperfThread[]  threads;
    private ExecutorService  threadPool;

    private int              interval;
    // ����һ�η��͵�ʱ��
    private long             earlyTime = 0;
    // ���һ�η���ʱ��
    private long             endTime   = 0;

    private FileWriter       fw        = null;
    private SimpleDateFormat sdf       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,ms");
    // ��һ�μ�¼
    private DataStatistics   lastData  = new DataStatistics();

    private String           logFile   = "simperf-result.log";

    public PrintStatus(SimperfThread[] threads, ExecutorService threadPool, int interval) {
        this.threads = threads;
        this.threadPool = threadPool;
        this.interval = interval;
    }

    public void run() {
        do {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outputMessage();
        } while (!threadPool.isTerminated());
        onExit();
    }

    public void openLogFile() {
        if (null == fw) {
            try {
                fw = new FileWriter(logFile, true);
                fw.write("======>\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onExit() {
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void outputMessage() {
        if (earlyTime <= 0 && threads.length > 0) {
            earlyTime = threads[0].getStatistics().startTime;
            for (int i = 1; i < threads.length; i++) {
                long t = threads[i].getStatistics().startTime;
                earlyTime = earlyTime > t ? t : earlyTime;
            }
        }
        DataStatistics allCalc = new DataStatistics();
        for (int i = 0; i < threads.length; i++) {
            DataStatistics data = threads[i].getStatistics();
            allCalc.failCount += data.failCount;
            allCalc.successCount += data.successCount;
            endTime = endTime > data.endTime ? endTime : data.endTime;
        }

        long duration = endTime - earlyTime;
        long count = allCalc.failCount + allCalc.successCount;
        String avgTps = SimperfUtil.divide(count * 1000, duration);

        String now = sdf.format(new Date());
        String msg = now + " , ALL [avgTps=" + avgTps + " ,count=" + count + " ,duration="
                     + duration + " ,fail=" + allCalc.failCount + "]";

        // ͳ��ʵʱ��Ϣ
        if (lastData.endTime != 0) {
            long tDuration = endTime - lastData.endTime;
            long tCount = count - lastData.successCount - lastData.failCount;
            long tFail = allCalc.failCount - lastData.failCount;
            String tTps = SimperfUtil.divide(tCount * 1000, tDuration);
            msg += " NOW [tTps=" + tTps + " ,tCount=" + tCount + " ,tDuration=" + tDuration
                   + " ,tFail=" + tFail + "]\n";
        } else {
            // ��һ��ͳ�ƣ�û���ϴμ�¼���
            msg += " NOW [tTps=" + avgTps + " ,tCount=" + count + " ,tDuration=" + duration
                   + " ,tFail=" + allCalc.failCount + "]\n";
        }

        write(msg);
        // ��¼�ϴν�������ڷ���ʵʱ��Ϣ
        lastData = allCalc;
        // �ϴν���е�endTimeΪ�ϴη���ʱ��
        lastData.endTime = endTime;
    }

    public void write(String msg) {
        try {
            openLogFile();
            System.out.print(msg);
            fw.write(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }
}