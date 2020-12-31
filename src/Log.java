package src;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Log extends Thread
{
    private BlockingQueue<String> coda;
    private String filename;
    private FileWriter fw;
    private Formatter writer;
    
    public Log(String filename)
    {
        this.setName("Logger");
        this.filename = "../lop/" + filename;
        this.coda = new LinkedBlockingQueue<>(64);
        try
        {
            this.fw = new FileWriter(this.filename, true);
            this.writer = new Formatter(this.fw);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void add_msg(String msg)
    {
        if (msg != null)
        {
            try
            {
                this.coda.put(msg);
            }
            catch (InterruptedException e) { }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void writeToFile(String msg)
    {
        try
        {
            LocalDateTime myDateObj = LocalDateTime.now();
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formattedDate = myDateObj.format(myFormatObj);

            if (msg != null)
            {
                this.writer.format("[ %s ] -- %s\n", formattedDate, msg);
                this.writer.flush();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        try
        {
            File lop_dir = new File("../lop");
            if (!lop_dir.exists())
            {
                lop_dir.mkdirs();
            }
            
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    this.writeToFile(this.coda.take());
                }
                catch (InterruptedException e) { }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void shutdown ()
    {
        if (!this.coda.isEmpty())
        {
            try
            {
                for (String msg : this.coda)
                {
                    this.writeToFile(msg);
                }

                this.writer.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}