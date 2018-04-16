package sky.toilettes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Collections;
import java.util.TimeZone;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.TTCCLayout;
import org.apache.log4j.helpers.DateTimeDateFormat;
import org.apache.log4j.spi.LoggingEvent;

public final class Logger
{
    public static final boolean LOGGER_ACTIVE=true;
    public static final org.apache.log4j.Logger LOGGER;

    static
    {
        org.apache.log4j.Logger tempLogger;
        try
        {
            tempLogger=org.apache.log4j.Logger.getRootLogger();
            TTCCLayout layout=new TTCCLayout()
            {
                public String format(LoggingEvent event)
                {
                    buf.setLength(0);
                    dateFormat(buf,event);
                    buf.append(event.getLevel().toString());
                    buf.append(" - ");
                    buf.append(event.getRenderedMessage());
                    buf.append(Layout.LINE_SEP);
                    return buf.toString();
                }
            };
            layout.setDateFormat(new DateTimeDateFormat(TimeZone.getDefault()),TimeZone.getDefault());
            tempLogger.addAppender(new DailyRollingFileAppender(layout,"log/log_"+InetAddress.getLocalHost().getHostName()+"_"+System.getProperty("user.name")+"_"+getPID()+".txt","'.'yyyy-MM-dd"));
            tempLogger.setLevel(Level.ALL);
            tempLogger.info("####################################################################################");
            tempLogger.info("Logging is starting");
            tempLogger.info("Log level selected: "+tempLogger.getLevel().toString());
            try
            {
                System.setErr(new LoggerBridge(true));
                System.setOut(new LoggerBridge(false));
            }
            catch(SecurityException e)
            {
                tempLogger.error("Unable to redirect the standard output streams into the logger");
            }
        }
        catch(Throwable t)
        {
            tempLogger=null;
            System.out.println("Unable to create the logger");
            t.printStackTrace();
            System.exit(0);
        }
        LOGGER=tempLogger;
        try
        {
            File[] logFiles=new File("log/").listFiles((dir,name)->name.startsWith("log_"));
            long now=System.currentTimeMillis();
            int count=0;
            for(File logFile:logFiles)
            {
                long modified=logFile.lastModified();
                if(modified!=0L&&now-modified>Time.get(30).day())
                {
                    logFile.delete();
                    count++;
                }
            }
            LOGGER.info(count+" log files have been deleted");
        }
        catch(Throwable t)
        {
            LOGGER.warn("Unable to complete the log cleaning ("+t.toString()+")");
        }
        LOGGER.info("OS version: "+System.getProperty("os.name"));
        LOGGER.info("Java version: "+System.getProperty("java.version")+" "+System.getProperty("sun.arch.data.model")+"-bit");
        LOGGER.info("Java home: "+System.getProperty("java.home"));
    }

    public static int getPID()
    {
        try
        {
            return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        }
        catch(Throwable t)
        {
            return -1;
        }
    }

    public static File getLogFile()
    {
        for(Object object:Collections.list(Logger.LOGGER.getAllAppenders()))
            if((object instanceof DailyRollingFileAppender))
                return new File(((DailyRollingFileAppender)object).getFile());
        return null;
    }

    private static class LoggerBridge extends PrintStream
    {
        private final boolean speakAsError;

        private LoggerBridge(boolean speakAsError)
        {
            super(new OutputStream()
            {
                public void write(int b) throws IOException
                {
                }
            });
            this.speakAsError=speakAsError;
        }

        @Override
        public void print(Object obj)
        {
            if(speakAsError)
                LOGGER.error(obj);
            else
                LOGGER.info(obj);
        }

        @Override
        public void print(String s)
        {
            if(speakAsError)
                LOGGER.error(s);
            else
                LOGGER.info(s);
        }

        @Override
        public void println()
        {
            if(speakAsError)
                LOGGER.error("");
            else
                LOGGER.info("");
        }

        @Override
        public void println(Object x)
        {
            if(speakAsError)
                LOGGER.error(x);
            else
                LOGGER.info(x);
        }

        @Override
        public void println(String x)
        {
            if(speakAsError)
                LOGGER.error(x);
            else
                LOGGER.info(x);
        }
    }
}
