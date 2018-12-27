package org.yamcs.sle;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.openmuc.jasn1.ber.types.BerObjectIdentifier;
import org.openmuc.jasn1.ber.types.string.BerVisibleString;

import ccsds.sle.transfer.service.bind.types.ApplicationIdentifier;
import ccsds.sle.transfer.service.bind.types.AuthorityIdentifier;
import ccsds.sle.transfer.service.bind.types.PortId;
import ccsds.sle.transfer.service.bind.types.SleBindInvocation;
import ccsds.sle.transfer.service.bind.types.SleBindReturn;
import ccsds.sle.transfer.service.bind.types.SleBindReturn.Result;
import ccsds.sle.transfer.service.bind.types.SlePeerAbort;
import ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import ccsds.sle.transfer.service.bind.types.VersionNumber;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuStartInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuUserToProviderPdu;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuAsyncNotifyInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuGetParameterReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuProviderToUserPdu;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStartReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStatusReportInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuThrowEventReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuTransferDataReturn;
import ccsds.sle.transfer.service.cltu.structures.CltuIdentification;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.service.instance.id.OidValues;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceAttribute;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceIdentifier;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class CltuServiceUserHandler extends AbstractServiceHandler {
    static public enum State {
        UNBOUND, BINDING, READY, STARTING, ACTIVE
    };

    final Isp1Authentication auth;
    final String initiatorId;
    final String responderPortId;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Isp1Handler.class);
    
    State state = State.UNBOUND;
    private CompletableFuture<Void> bindingCf;
    private CompletableFuture<Void> startingCf;
    
    public CltuServiceUserHandler(Isp1Authentication auth, String responderPortId, String initiatorId) {
        this.initiatorId = initiatorId;
        this.responderPortId = responderPortId;
        this.auth = auth;
    }

    
    public CompletableFuture<Void> bind() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendBind(cf));
        return cf;
    }
   
    
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStart(cf));
        return cf;
    }
   
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("received message "+msg);
        if(logger.isTraceEnabled()) {
            logger.trace( "received message: {}", msg);
        }
        CltuProviderToUserPdu pdu = (CltuProviderToUserPdu) msg;
        if(pdu.getCltuTransferDataReturn()!=null) {
            processTransferDataReturn(pdu.getCltuTransferDataReturn());
        } else if(pdu.getCltuBindReturn()!=null) {
            processBindReturn(pdu.getCltuBindReturn());
        } else if(pdu.getCltuAsyncNotifyInvocation()!=null) {
            processAsyncNotifyInvocation(pdu.getCltuAsyncNotifyInvocation());
        } else if(pdu.getCltuGetParameterReturn()!=null) {
            processGetParameterReturn(pdu.getCltuGetParameterReturn());
        } else if(pdu.getCltuPeerAbortInvocation()!=null) {
            processPeerAbortInvocation(pdu.getCltuPeerAbortInvocation());
        } else if(pdu.getCltuScheduleStatusReportReturn()!=null) {
            processCltuScheduleStatusReportReturn(pdu.getCltuScheduleStatusReportReturn());
        } else if(pdu.getCltuStartReturn()!=null) {
            processStartReturn(pdu.getCltuStartReturn());
        }  else if(pdu.getCltuStatusReportInvocation()!=null) {
            processStatusReportInvocation(pdu.getCltuStatusReportInvocation());
        }  else if(pdu.getCltuStopReturn()!=null) {
            processStopReturn(pdu.getCltuStopReturn());
        } else if(pdu.getCltuThrowEventReturn()!=null) {
            processThrowEventReturn(pdu.getCltuThrowEventReturn());
        } else if(pdu.getCltuUnbindReturn()!=null) {
            processCltuUnbindReturn(pdu.getCltuUnbindReturn());
        } else {
            logger.error("Unexpected state");
            throw new IllegalStateException();
        }
        
       
    }

   
    private void processCltuUnbindReturn(SleUnbindReturn cltuUnbindReturn) {
        // TODO Auto-generated method stub
        
    }

    private void processThrowEventReturn(CltuThrowEventReturn cltuThrowEventReturn) {
        // TODO Auto-generated method stub
        
    }

    private void processStopReturn(SleAcknowledgement cltuStopReturn) {
        // TODO Auto-generated method stub
        
    }

    private void processStatusReportInvocation(CltuStatusReportInvocation cltuStatusReportInvocation) {
        // TODO Auto-generated method stub
        
    }

   
    private void processCltuScheduleStatusReportReturn(SleScheduleStatusReportReturn cltuScheduleStatusReportReturn) {
        // TODO Auto-generated method stub
        
    }

    private void processPeerAbortInvocation(SlePeerAbort cltuPeerAbortInvocation) {
        // TODO Auto-generated method stub
        
    }

    private void processGetParameterReturn(CltuGetParameterReturn cltuGetParameterReturn) {
        // TODO Auto-generated method stub
        
    }

    private void processAsyncNotifyInvocation(CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation) {
        // TODO Auto-generated method stub
        
    }

    private void sendBind(CompletableFuture<Void> cf) {
        if(state!=State.UNBOUND) {
            cf.completeExceptionally(new SleException("Cannot call bind while in state "+state));
            return;
        }
        state = State.BINDING;
        this.bindingCf = cf;
        
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        SleBindInvocation sbi = new SleBindInvocation();
        sbi.setInitiatorIdentifier(new AuthorityIdentifier(initiatorId.getBytes(StandardCharsets.US_ASCII)));
        sbi.setInvokerCredentials(auth.generateCredentials());

        sbi.setServiceInstanceIdentifier(getServiceInstanceIdentifier());
        sbi.setResponderPortIdentifier(new PortId(responderPortId.getBytes(StandardCharsets.US_ASCII)));
        sbi.setVersionNumber(new VersionNumber(2));
        sbi.setServiceType(new ApplicationIdentifier(Constants.APP_ID_FWD_CLTU));
        cutp.setCltuBindInvocation(sbi);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processBindReturn(SleBindReturn cltuBindReturn) {
        if(state != State.BINDING) {
            peerAbort();
            return;
        }
        Result r = cltuBindReturn.getResult();
        if(r.getNegative()!=null) {
            state = State.UNBOUND;
            bindingCf.completeExceptionally(new SleException("bind failed: "+Constants.BIND_DIAGNOSTIC.get(r.getNegative().intValue())));
        } else {
            state = State.READY;
            bindingCf.complete(null);
        }
    }

    private void peerAbort() {
        // TODO Auto-generated method stub
        
    }

    private void processTransferDataReturn(CltuTransferDataReturn cltuTransferDataReturn) {
        // TODO Auto-generated method stub
        
    }


    private void sendStart(CompletableFuture<Void> cf) {
        if(state!=State.READY) {
            cf.completeExceptionally(new IllegalStateException("Cannot call start while in state "+state));
            return;
        }
        state = State.STARTING;
        this.startingCf = cf;
        
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuStartInvocation csi = new CltuStartInvocation();
        csi.setFirstCltuIdentification(new CltuIdentification(1));
        csi.setInvokeId(new InvokeId(1));
        csi.setInvokerCredentials(auth.generateCredentials());
        
        cutp.setCltuStartInvocation(csi);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processStartReturn(CltuStartReturn cltuStartReturn) {
        ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStartReturn.Result r = cltuStartReturn.getResult();
        if(r.getNegativeResult()!=null) {
            startingCf.completeExceptionally(new SleException("failed to start", r.getNegativeResult()));
            state = State.READY;
        } else {
            startingCf.complete(null);
            state = State.ACTIVE;
        }
    }

    private static ServiceInstanceIdentifier getServiceInstanceIdentifier() {
        ServiceInstanceIdentifier sii = new ServiceInstanceIdentifier();
        List<ServiceInstanceAttribute> l = sii.getServiceInstanceAttribute();
        l.add(getServiceInstanceAttribute(OidValues.sagr, "SAGR"));
        l.add(getServiceInstanceAttribute(OidValues.spack, "SPACK"));
        l.add(getServiceInstanceAttribute(OidValues.fslFg, "FSL-FG"));
        l.add(getServiceInstanceAttribute(OidValues.cltu, "cltu2"));
        return sii;
    }

    private static ServiceInstanceAttribute getServiceInstanceAttribute(BerObjectIdentifier id, String value) {
        ServiceInstanceAttribute sia = new ServiceInstanceAttribute();
        ServiceInstanceAttribute.SEQUENCE sias = new ServiceInstanceAttribute.SEQUENCE();
        sias.setIdentifier(id);
        sias.setSiAttributeValue(new BerVisibleString(value));
        sia.getSEQUENCE().add(sias);
        return sia;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //TODO
        cause.printStackTrace();
        ctx.close();
    }
    
    
}
