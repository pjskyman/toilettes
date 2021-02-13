package sky.toilettes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalControllerClient implements Runnable
{
    private final ExternallyControlled externallyControlled;
    private final Socket socket;

    public ExternalControllerClient(ExternallyControlled externallyControlled,Socket socket)
    {
        this.externallyControlled=externallyControlled;
        this.socket=socket;
    }

    public void run()
    {
        while(!socket.isClosed())
        {
            try
            {
                PrintWriter writer=new PrintWriter(socket.getOutputStream());
                BufferedInputStream inputStream=new BufferedInputStream(socket.getInputStream());
                String request=read(inputStream);
                if(request.equals("ON"))
                {
                    externallyControlled.on();
                    writer.write("OK");
                }
                else
                    if(request.equals("OFF"))
                    {
                        externallyControlled.off();
                        writer.write("OK");
                    }
                    else
                        if(request.startsWith("SET"))
                        {
                            Matcher matcher=Pattern.compile("SET;(.*);(.*)").matcher(request);
                            if(matcher.matches())
                            {
                                String group1=matcher.group(1);
                                double setPoint=Double.parseDouble(group1.replace(",","."));
                                String group2=matcher.group(2);
                                double hours=Double.parseDouble(group2.replace(",","."));
                                externallyControlled.set(setPoint,hours);
                                writer.write("OK");
                            }
                            else
                                writer.write("NOK");
                        }
                        else
                            if(request.equals("RESET"))
                            {
                                externallyControlled.reset();
                                writer.write("OK");
                            }
                            else
                                writer.write("NOK");
                writer.flush();
                socket.close();
            }
            catch(Exception e)
            {
                Logger.LOGGER.error("Error when processing client request: "+e.toString());
            }
        }
    }

    private static String read(InputStream inputStream) throws IOException
    {
        String request="";
        int length;
        byte[] data=new byte[4096];
        length=inputStream.read(data);
        request=new String(data,0,length);
        return request;
    }
}
