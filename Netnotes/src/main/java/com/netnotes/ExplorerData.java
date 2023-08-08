package com.netnotes;

import java.util.concurrent.TimeUnit;

import com.utils.Utils;

public class ExplorerData {

    private ErgoWallet m_ergoWallet;
    String m_explorerId;
    private long m_period;
    private TimeUnit m_timeUnit;

    public ExplorerData(ErgoWallet ergoWallet, String explorerId, long period, TimeUnit timeUnit) {
        m_ergoWallet = ergoWallet;
        m_explorerId = explorerId;
        m_period = period;
        m_timeUnit = timeUnit;
    }

    public NoteInterface getExplorerInterface() {
        return m_ergoWallet.getErgoNetworkData().getNetwork(m_explorerId);
    }

    public long getPeriod() {
        return m_period;
    }

    public TimeUnit getTimeUnit() {
        return m_timeUnit;
    }

    @Override
    public String toString() {

        return m_period + Utils.timeUnitToString(m_timeUnit);
    }
}
