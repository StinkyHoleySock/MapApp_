package com.example.mapapp

import android.util.Log
import java.io.*
import java.net.Socket

class TcpClient(listener: OnMessageReceived) {

    private var serverMessage: String? = null
    private var messageListener: OnMessageReceived? = null
    private var run = false
    private var bufferOut: PrintWriter? = null
    private var bufferIn: BufferedReader? = null
    private var fileReader: FileReader? = null
    private var text = ""

    init {
        messageListener = listener
    }

    fun sendMessage(message: String?) {
        if (bufferOut != null && !bufferOut!!.checkError()) {
            bufferOut!!.println(message)
            bufferOut!!.flush()
        }
    }

    fun stopClient() {
        sendMessage("end")
        run = false
        if (bufferOut != null) {
            bufferOut!!.flush()
            bufferOut!!.close()
        }
        messageListener = null
        bufferIn = null
        bufferOut = null
        serverMessage = null
    }

    fun run() {
        run = true
        try {
            val socket = Socket("192.168.0.5", 7777)
            try {
                bufferOut = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                bufferIn = BufferedReader(InputStreamReader(socket.getInputStream()))
                sendMessage("test")

                while (run) {
                    serverMessage = bufferIn!!.readLine()
                    if (serverMessage != null) {
                        messageListener!!.messageReceived(serverMessage)
                        text += "$serverMessage\n"

                        if (serverMessage == "") {
                            messageListener!!.saveFileInStorage(text)
                        }
                        Log.d("develop", "message: $serverMessage")
                        Log.d("develop", "all text: $text")
                    }
                }
            } catch (e: Exception) {
                Log.e("TCP", "S: Error", e)
            } finally {

                socket.close()
            }
        } catch (e: Exception) {
            Log.e("TCP", "C: Error", e)
        }
    }

    interface OnMessageReceived {
        fun messageReceived(message: String?)
        fun saveFileInStorage(text: String)
    }
}