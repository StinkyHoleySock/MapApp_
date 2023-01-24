package com.example.mapapp

import android.util.Log
import java.io.*
import java.net.Socket

class TcpClient(listener: OnMessageReceived) {

    private var mServerMessage: String? = null
    private var mMessageListener: OnMessageReceived? = null
    private var mRun = false
    private var mBufferOut: PrintWriter? = null
    private var mBufferIn: BufferedReader? = null

    init {
        mMessageListener = listener
    }

    fun sendMessage(message: String?) {
        if (mBufferOut != null && !mBufferOut!!.checkError()) {
            mBufferOut!!.println(message)
            mBufferOut!!.flush()
        }
    }

    fun stopClient() {

        // send mesage that we are closing the connection
        sendMessage("end")
        mRun = false
        if (mBufferOut != null) {
            mBufferOut!!.flush()
            mBufferOut!!.close()
        }
        mMessageListener = null
        mBufferIn = null
        mBufferOut = null
        mServerMessage = null
    }

    fun run() {
        mRun = true
        try {
            val socket = Socket("192.168.0.5", 7777)
            try {

                mBufferOut =
                    PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)

                mBufferIn = BufferedReader(InputStreamReader(socket.getInputStream()))
                sendMessage("test")

                while (mRun) {
                    mServerMessage = mBufferIn!!.readLine()
                    if (mServerMessage != null ) {
                        mMessageListener!!.messageReceived(mServerMessage)
                        Log.d("develop", "message: $mServerMessage")
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
    }
}