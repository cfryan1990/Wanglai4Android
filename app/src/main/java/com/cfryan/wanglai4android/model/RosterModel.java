package com.cfryan.wanglai4android.model;

/**
 * Created by feng on 2015/8/30.
 */
public class RosterModel
{
    private String jid;
    private String alias;
    private String statusMode;
    private String statusMessage;

    public String getJid()
    {
        return jid;
    }

    public void setJid(String jid)
    {
        this.jid = jid;
    }

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public String getStatusMode()
    {
        return statusMode;
    }

    public void setStatusMode(String statusMode)
    {
        this.statusMode = statusMode;
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

    public void setStatus_message(String statusMessage)
    {
        this.statusMessage = statusMessage;
    }
}
