package org.yamcs.sle;

/**
 * SLE Parameter names as specified in the common-types ASN.1 definition
 *
 */
public enum ParameterName implements SleEnum {
    acquisitionSequenceLength(201, "The size, in octets, of the bit pattern to be radiated to enable"
            + "the spacecraft telecommand system to achieve bit lock. "), //
    apidList(2, "List of APIDs the given service instance is authorized to access."), //
    bitLockRequired(3, "If the value is ‘yes’, the ‘No bit lock’ flag in the CLCW must be"
            + "false in order for the provider to set production-status to"
            + "‘operational’. "), //
    blockingTimeoutPeriod(0, "Period (in milliseconds) from inserting the first Packet into the"
            + "Frame Data Unit until this unit is passed to the FOP regardless of"
            + "the number of Packets contained; this timeout period is applicable"
            + "to all blocking regardless of the specific MAP. "), //
    blockingUsage(1, "‘permitted’ if the provider may block multiple Space Packets into a"
            + "single frame, ‘not permitted’ otherwise; the permission to block"
            + "Space Packets applies to all MAPs in use on the given VC."), //
    bufferSize(4, "The size of the transfer buffer: the value of this parameter"
            + "shall indicate the number of RAF/RCF transfer-data and"
            + "RAF/RCF sync-notify invocations that can be stored in the"
            + "transfer buffer."), //
    clcwGlobalVcId(202, "The Master or Virtual Channel that carries the CLCW to be"
            + "used by the F-CLTU/FSP provider to determine the forward link"
            + "RF and/or bit lock status. If the configuration of the given"
            + "service instance is such that the CLCW shall not be"
            + "evaluated then the parameter value reported is ‘not"
            + "configured’."), //
    clcwPhysicalChannel(203, "The RF return channel that carries the CLCW to be used by"
            + "the F-CLTU/FSP provider to determine the forward link RF and bit"
            + "lock status. If the configuration of the given service instance"
            + "is such that the CLCW shall not be evaluated then the"
            + "parameter value reported is ‘not configured’."), //
    copCntrFramesRepetition(300, "The number of times a BC frame on the given VC is passed to"
            + "the Forward CLTU Generation FG."), //
    deliveryMode(6, "rtnTimelyOnline(0), rtnCompleteOnline(1), rtnOffline(2), fwdOnline(3) or fwdOffline(4)"), //
    directiveInvocationOnline(108, "‘yes’, if the service instance that is permitted to invoke directives"
            + "for the given VC if any is in the state ‘ready’ or ‘active’, ‘no’, if no"
            + "such service exists or if the service instance that is permitted to"
            + "invoke directives is in the state ‘unbound’. "), //
    expectedDirectiveIdentification(8, "The directive-identification parameter value the"
            + "provider expects in the next FSP-INVOKE-DIRECTIVE"
            + "invocation. The initial value of this parameter is zero. "), //
    expectedEventInvocationIdentification(9, "The expected value of the event-invocationidentification parameter "
            + "to be received in the next throw event invocation. The initial value of this parameter is zero. "), //
    expectedSlduIdentification(10, "The packet-identification parameter value the provider"
            + "expects in the next FSP-TRANSFER-DATA invocation; If no FSPSTART operation has been performed yet when the GET"
            + "operation is invoked, zero shall be returned as the default value"
            + "of this parameter."), //
    fopSlidingWindow(11, "Number of frames that can be transmitted on the given VC before"
            + "an acknowledgement is required. "), //
    fopState(12, "State of the FOP on the given VC:"
            + "‘ACTIVE’,"
            + "‘RETRANSMIT WITHOUT WAIT’,"
            + "‘RETRANSMIT WITH WAIT’,"
            + "‘INITIALIZING WITHOUT BC FRAME’,"
            + "‘INITIALIZING WITH BC FRAME’,"
            + "‘INITIAL’. "), //
    latencyLimit(15, "The maximum allowable delivery latency time (in seconds) for the online delivery mode (i.e., the"
            + "maximum delay from when the frame is acquired by the provider until the RAF/RCF extracted from it is delivered to the"
            + "user): the value of this parameter shall be ‘null’ if the delivery mode is offline."), //
    mapList(16, "List of MAPs permitted to be used by the given service instance if"
            + "MAPs are used, ‘null’ otherwise. "), //
    mapMuxControl(17, "MAP priority list or MAP polling vector. If the map-multiplexingscheme is ‘FIFO’ or if MAPs "
            + "are not used, this parameter is ‘null’. This parameter applies to the given VC. "), //
    mapMuxScheme(18, "MAP multiplexing scheme in effect on the given VC: ‘FIFO’, "
            + "‘absolute priority’, ‘polling vector’. If no MAP multiplexing is used, "
            + "this parameter is set to ‘FIFO’."), //
    maximumFrameLength(19, "Mission maximum TC transfer frame length in octets."), //
    maximumPacketLength(20, "Mission maximum Packet Data Length in octets."), //
    maximumSlduLength(21, "The size, in octets, of the maximum-length CLTU/RAF/RCF that will be"
            + "accepted by the provider for this service instance. "), //
    minimumDelayTime(204, "The minimum guard time the F-CLTU provider will accept"
            + "between two consecutive CLTUs. "), //
    minReportingCycle(301, "The minimum setting (in seconds) of the reporting cycle for"
            + "status reports that the service user may request in"
            + "an schedule status report invocation. "), //
    modulationFrequency(22, "The subcarrier frequency (when applicable) or the frequency"
            + "of direct on-carrier data modulation, expressed in tenths of"
            + "Hertz. "), //
    modulationIndex(23, "The angle by which the RF carrier is phase shifted with"
            + "respect to the un-modulated RF carrier, expressed in milliradians (10-3 rad). "), //
    notificationMode(205, "The notification-mode may be ‘immediate’ or ‘deferred’. If"
            + "‘immediate’, the user is notified of a production-status"
            + "change to ‘interrupted’ by means of a CLTU-ASYNC-NOTIFY"
            + "invocation as soon as this transition is detected. If ‘deferred’,"
            + "the user is notified about the production-status change"
            + "by means of a CLTU-ASYNC-NOTIFY invocation only if and"
            + "when the radiation of a CLTU is affected. "), //
    permittedControlWordTypeSet(101, "The set of control-word-type values permitted for this"
            + "ROCF service instance"), //
    permittedFrameQuality(302, "The set of frame quality criteria that the RAF service user can"
            + "choose from to select which frames the RAF service provider"
            + "shall deliver. The set contains at least one of the following"
            + "options: ‘good frames only’, ‘erred frames only’, or ‘all"
            + "frames’. "), //
    permittedGvcidSet(24, "The MCID and/or the set of global VCIDs permitted for this RCF service instance."), //
    permittedTcVcidSet(102, "The set of tc-vcid values permitted for this ROCF service"
            + "the value is ‘null’ if selection of a telecommand"
            + "VC is not permissible, e.g., because control words whose"
            + "type is not ‘clcw’ are to be delivered. "), //
    permittedTransmissionMode(107, "Specifies the transmission mode permitted for the given service"
            + "instance; it may be ‘expedited’ or ‘sequence-controlled’ or ‘any’."), //
    permittedUpdateModeSet(103, "The set of update-mode values permitted for this ROCF"
            + "service instance"), //
    plop1IdleSequenceLength(206, "The size, in octets, of the optional idle sequence that shall be"
            + "used in conjunction with PLOP-1. If 0, no idle sequence is"
            + "applied. "), //
    plopInEffect(25, "The physical layer operation procedure (PLOP) being used:"
            + "‘PLOP-1’ or ‘PLOP-2’. "), //
    protocolAbortMode(207, "The protocol-abort-mode may be ‘abort’ or ‘continue’. If"
            + "it is ‘abort’, service production shall cease in the event of a"
            + "protocol abort. If it is ‘continue’, service production shall"
            + "disregard this event and continue radiating the CLTUs"
            + "already buffered at that time. "), //
    reportingCycle(26, "The current setting of the reporting cycle for status reports; the value is ‘null’ "
            + "if cyclic reporting is off, otherwise it is the time (in seconds) between successive status "
            + "report invocations. As long as the service user has not yet set this parameter by means of a "
            + "successful schedule status report operation, its value shall be ‘null’. "), //
    requestedControlWordType(104, "The control word type requested by the most recent ROCFSTART operation if "
            + "the service instance is in the ‘active’ state; otherwise the first element of the"
            + "permitted-control-word-type-set parameter. "), //
    requestedFrameQuality(27, "The frame quality criteria, set by the RAF-START operation,"
            + "used to determine which frames are selected for delivery. As"
            + "long as the user has not yet set the value of this parameter"
            + "by means of a successful RAF-START invocation, its value"
            + "shall be that of the first element of the permitted-framequality-set parameter."), //
    requestedGvcid(28, "If the provider is in state 3 (‘active’), the GVCID set by the"
            + "RCF-START operation, used to determine which frames are"
            + "selected for delivery. If the provider is not in state 3 (‘active’),"
            + "the GVCID value returned shall be the first element of the permitted-global-VCID-set parameter. "), //
    requestedTcVcid(105, "The tc-vcid value requested by the most recent ROCFSTART operation, if the service "
            + "instance is in the ‘active’ state: the value is ‘null’ if selection of a"
            + "telecommand VC is not permissible, e.g., because control"
            + "words whose type is not ‘clcw’ are to be delivered. If the"
            + "service instance is not in the ‘active’ state, the value reported"
            + "shall be the first element of the permitted-tc-vcid-set"
            + "parameter. "), //
    requestedUpdateMode(106, "The update-mode value requested by the most recent"
            + "ROCF-START operation (see 3.4.2.10) if the service"
            + "instance is in the ‘active’ state; otherwise the first element of"
            + "the permitted-update-mode-set parameter. "), //
    returnTimeoutPeriod(29, "The maximum time period (in seconds) permitted from when a confirmed operation "
            + "is invoked until the return is received by the invoker. "), //
    rfAvailableRequired(31, "If the value is ‘yes’, the ‘No RF available’ flag in the CLCW"
            + "must be false in order for the provider to set productionstatus to ‘operational’."), //
    segmentHeader(32, "Specifies if a Segment Header is ‘present’ or ‘absent’ in the TC"
            + "transfer frames."), //
    sequCntrFramesRepetition(303, "The number of times an AD frame on the given VC is passed to"
            + "the Forward CLTU Generation FG."), //
    subcarrierToBitRateRatio(34, "When subcarrier modulation is used, the value represents"
            + "the ratio of the subcarrier frequency to the uplink data rate"
            + "(i.e., the bit rate). A value of one indicates that data will be"
            + "directly modulated onto the carrier."), //
    throwEventOperation(304, "‘enabled’ if this service instance is authorized to invoke the throw-event operation, "
            + "‘disabled’ otherwise. "), //
    timeoutType(35, "Specifies FOP behavior (either ‘Alert’ or ‘AD service suspension’)."), //
    timerInitial(36, "Initial value (in microseconds) for countdown timer when an AD or"
            + "BC frame is transmitted."), //
    transmissionLimit(37, "Maximum number of times the first frame on the Sent_Queue"
            + "may be transmitted "), //
    transmitterFrameSequenceNumber(38, "After a transmission mode capability change event, the parameter"
            + "Transmitter_Frame_Sequence_Number, V(S), contains the value"
            + "of the Frame Sequence Number, N(S), to be put in the Transfer"
            + "Frame Primary Header of the next Type-AD frame to be"
            + "transmitted."), //
    vcMuxControl(39, "VC priority list or the VC polling vector. If the vc-multiplexing"
            + "scheme is ‘FIFO’, this parameter is ‘null’."), //
    vcMuxScheme(40, "VC multiplexing scheme in effect: ‘FIFO’, ‘absolute priority’,"
            + "‘polling vector’."), //
    virtualChannel(41, "VC being used by this service instance.");

    private final int id;
    private final String description;

    private ParameterName(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int id() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    static public ParameterName byId(int id) {
        for (ParameterName v : values()) {
            if (v.id == id) {
                return v;
            }
        }
        throw new IllegalArgumentException("invalid id " + id);
    }
}
