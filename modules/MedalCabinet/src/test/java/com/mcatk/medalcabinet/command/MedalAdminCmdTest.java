package com.mcatk.medalcabinet.command;

import com.mcatk.medalcabinet.MedalCabinet;
import com.mcatk.medalcabinet.sql.SQLManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.*;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedalAdminCmdTest {
    @Test void queuedRequestsRetainTheirOwnPlayerArgumentsAndSender() {
        MedalCabinet plugin=mock(MedalCabinet.class);SQLManager sql=mock(SQLManager.class);
        List<Supplier<?>> tasks=new ArrayList<>();List<Consumer<Object>> callbacks=new ArrayList<>();
        doAnswer(call->{tasks.add(call.getArgument(0));callbacks.add(call.getArgument(1));return null;})
                .when(plugin).queryAsync(any(),any(),any());
        CommandSender first=mock(CommandSender.class),second=mock(CommandSender.class);
        when(first.isOp()).thenReturn(true);when(second.isOp()).thenReturn(true);
        try(MockedStatic<MedalCabinet> plugins=mockStatic(MedalCabinet.class);MockedStatic<SQLManager> managers=mockStatic(SQLManager.class)) {
            plugins.when(MedalCabinet::getPlugin).thenReturn(plugin);managers.when(SQLManager::getInstance).thenReturn(sql);
            MedalAdminCmd command=new MedalAdminCmd();
            String[] args={"add","Alice","one"};
            command.onCommand(first,mock(Command.class),"medaladmin",args);
            command.onCommand(second,mock(Command.class),"medaladmin",new String[]{"add","Bob","two"});
            args[1]="Mallory";
            verify(sql,never()).addPlayerMedal(anyString(),anyString());
            Object secondResult=tasks.get(1).get(),firstResult=tasks.get(0).get();
            verify(sql).addPlayerMedal("Alice","one");verify(sql).addPlayerMedal("Bob","two");
            verify(first,never()).sendMessage(anyString());
            callbacks.get(1).accept(secondResult);callbacks.get(0).accept(firstResult);
            verify(first).sendMessage("Ok");verify(second).sendMessage("Ok");
        }
    }
}
