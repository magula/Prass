/*
 * MIT License
 *
 * Copyright (c) 2020 Manuel Gundlach <manuel.gundlach@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dowscr.manuel.prass;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Random;

/**
 * Execute SSH-commandos    via executeRemoteCommand or
 * copy files to host       via copyFiletoHost
 */
class ConnectionHandler {
    private static String username, password;
    static private Session session;

    static void setCreds(String user, String pass) {
        username = user;
        password = pass;
    }

    private synchronized static void TryConnect(String hostname) throws Exception {
        JSch jsch = new JSch();
        session = jsch.getSession(username, hostname, 22);
        session.setPassword(password);

        // Avoid asking for key confirmation
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);

        session.connect();
    }

    private synchronized static void Establish() throws Exception {
        if ((new Random()).nextBoolean())
            try {
                TryConnect("cip91.math.lmu.de");
            } catch (Exception e) {
                TryConnect("cip90.math.lmu.de");
            }
        else
            try {
                TryConnect("cip90.math.lmu.de");
            } catch (Exception e) {
                TryConnect("cip91.math.lmu.de");
            }
    }

    private static boolean isConnected() {
        return !(session == null || !session.isConnected());
    }

    static void assureConnection() throws Exception {
        if (!isConnected())
            Establish();
    }

    private synchronized static String tryexecuteRemoteCommand(
            String command) throws Exception {

        assureConnection();

        // SSH Channel
        ChannelExec channelssh = (ChannelExec)
                session.openChannel("exec");

        //Piping
        channelssh.setErrStream(System.err);
        channelssh.setInputStream(null);
        InputStream in = channelssh.getInputStream();

        // Execute command
        channelssh.setCommand(command);
        channelssh.connect();

        //Catching Return
        String Re = "";
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                Re += (new String(tmp, 0, i));
            }
            if (channelssh.isClosed()) {
                if (in.available() > 0) continue;
                System.out.println("exit-status: " + channelssh.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        channelssh.disconnect();
//        session.disconnect();

        return Re + "\n";
    }

    synchronized static String executeRemoteCommand(
            String command) throws Exception {
        for (int i = 0; ; ) {
            try {
                return tryexecuteRemoteCommand(command);
            } catch (Exception e) {
//                e.printStackTrace();
                i++;
                //Increase the number if you want it to try more than once.
                if (i == 1)
                    throw e;
            }
        }
    }

    synchronized static void copyFiletoHost(
            String file,
            InputStream fis,
            String TS) throws Exception {
        for (int i = 0; ; ) {
            try {
                trycopyFiletoHost(file, fis, TS);
                return;
            } catch (Exception e) {
//                e.printStackTrace();
                i++;
                //Increase the number if you want it to try more than once.
                if (i == 1)
                    throw e;
            }
        }
    }

    private synchronized static void trycopyFiletoHost(
            String file,
            InputStream fis,
            String TS) throws Exception {
        try {

            assureConnection();

            String rfile = "\"" + TS + file + "\"";
            //.rempri42 might have to exist...?

            // exec 'scp -t rfile' remotely
            String command = "scp " + " -t " + rfile;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if (checkAck(in) != 0) {
                throw new Exception();
            }

            // send "C0644 filesize filename", where filename should not include '/'

            //TODO THIS IS NO GOOD.
            long filesize = fis.available();//using .available() might be a bad idea.

            command = "C0644 " + filesize + " ";
            command += rfile;
            command += "\n";
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                throw new Exception();
            }

            // send a content of file
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) break;
                out.write(buf, 0, len); //out.flush();
            }
            fis.close();
            fis = null;
            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
            if (checkAck(in) != 0) {
                throw new Exception();
            }
            out.close();

            channel.disconnect();
//            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (fis != null) fis.close();
            } catch (IOException ee) {
                e.printStackTrace();
            }
            throw e;
        }
    }

    private synchronized static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

}
