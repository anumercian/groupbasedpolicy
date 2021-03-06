/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.ChainActionFlows.createChainTunnelFlows;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxSetNsiAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxSetNspAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ChainAction.isSrcEpConsumer;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.PolicyPair;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcIidFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader.SfcNshHeaderBuilder;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Chain action for the OpenFlow Overlay renderer
 * TODO: separate the generic definition from the concrete
 * implementation for the OpenFlow Ovelray renderer
 */
public class ChainAction extends Action {

    private static final Logger LOG = LoggerFactory.getLogger(ChainAction.class);

    public static final ActionDefinitionId ID = new ActionDefinitionId("3d886be7-059f-4c4f-bbef-0356bea40933");

    public static final Integer CHAIN_CONDITION_GROUP = 0xfffffe;

    protected static final String TYPE = "type";

    // the chain action
    public static final String SFC_CHAIN_ACTION = "chain";
    // the parameter used for storing the chain name
    public static final String SFC_CHAIN_NAME = "sfc-chain-name";

    protected static final ActionDefinition DEF = new ActionDefinitionBuilder().setId(ID)
        .setName(new ActionName(SFC_CHAIN_ACTION))
        .setDescription(new Description("Send the traffic through a Service Function Chain"))
        .setParameter(
                (ImmutableList.of(new ParameterBuilder().setName(new ParameterName(SFC_CHAIN_NAME))
                    .setDescription(new Description("The named chain to match against"))
                    .setIsRequired(IsRequired.Required)
                    .setType(Type.String)
                    .build())))
        .build();

    @Override
    public ActionDefinitionId getId() {
        return ID;
    }

    @Override
    public ActionDefinition getActionDef() {
        return DEF;
    }

    @Override
    public List<ActionBuilder> updateAction(List<ActionBuilder> actions, Map<String, Object> params, Integer order,
            NetworkElements netElements, PolicyPair policyPair, FlowMap flowMap, OfContext ctx, Direction direction) {
        /*
         * Get the named chain
         */
        String chainName = null;
        if (params != null) {
            LOG.debug("updateAction: Searching for named chain");
            for (String name : params.keySet()) {
                if (name instanceof String) {
                    if (name.equals(SFC_CHAIN_NAME)) {
                        chainName = (String) params.get(name);
                        if (chainName == null) {
                            LOG.error("updateAction: Chain name was null");
                            return null;
                        }
                    }
                }
            }
        } else {
            LOG.error("updateAction: Parameters null for chain action");
            return null;
        }

        if (chainName == null) {
            LOG.error("updateAction: Chain name was null");
            return null;
        }

        /*
         * If path is symmetrical then there are two RSPs.
         * if srcEp is in consumer EPG use "rspName"
         * else srcEp is in provider EPG, "rspName-Reverse".
         */
        ServiceFunctionPath sfcPath = getSfcPath(chainName);
        if (sfcPath == null || sfcPath.getName() == null) {
            LOG.error("updateAction: SFC Path was invalid. Either null or name was null.", sfcPath);
            return null;
        }
        // Find existing RSP based on following naming convention, else create it.
        String rspName = sfcPath.getName() + "-gbp-rsp";
        ReadOnlyTransaction rTx = ctx.getDataBroker().newReadOnlyTransaction();
        RenderedServicePath renderedServicePath;
        RenderedServicePath rsp = getRspByName(rspName, rTx);
        if (rsp == null) {
            renderedServicePath = createRsp(sfcPath, rspName);
            if (renderedServicePath != null) {
                LOG.info("updateAction: Could not find RSP {} for Chain {}, created.", rspName, chainName);
            } else {
                LOG.error("updateAction: Could not create RSP {} for Chain {}", rspName, chainName);
                return null;
            }
        } else {
            renderedServicePath = rsp;
        }

        NodeId tunnelDestNodeId;
        if (netElements.getDstNodeId().equals(netElements.getLocalNodeId())) {
            // Return destination is here
            tunnelDestNodeId = netElements.getLocalNodeId();
        } else {
            tunnelDestNodeId = netElements.getDstNodeId();
        }

        Long returnVnid = (long) netElements.getSrcEpOrds().getTunnelId();

        try {
            if (sfcPath.isSymmetric() && direction.equals(Direction.Out)){
                rspName = rspName + "-Reverse";
                rsp = getRspByName(rspName, rTx);
                if (rsp == null) {
                    LOG.info("updateAction: Could not find Reverse RSP {} for Chain {}", rspName, chainName);
                    renderedServicePath = createSymmetricRsp(renderedServicePath);
                    if (renderedServicePath == null) {
                        LOG.error("updateAction: Could not create RSP {} for Chain {}", rspName, chainName);
                        return null;
                    }
                } else {
                    renderedServicePath = rsp;
                }
                if(isSrcEpConsumer(netElements.getSrcEp(), policyPair.getConsumerEpgId(), ctx)) {
                    if (netElements.getSrcNodeId().equals(netElements.getLocalNodeId())) {
                        // Return destination is here
                        tunnelDestNodeId = netElements.getLocalNodeId();
                    } else {
                        tunnelDestNodeId = netElements.getSrcNodeId();
                    }
                    returnVnid = (long) netElements.getDstEpOrds().getTunnelId();
                }
            }
        } catch (Exception e) {
            LOG.error("updateAction: Attemping to determine if srcEp {} was consumer.", netElements.getSrcEp().getKey(), e);
            return null;
        }

        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(rspName);
        if (!isValidRspFirstHop(rspFirstHop)) {
            // Errors logged in method.
            return null;
        }

        IpAddress tunnelDest = ctx.getSwitchManager().getTunnelIP(tunnelDestNodeId, TunnelTypeVxlanGpe.class);
        if (tunnelDest == null || tunnelDest.getIpv4Address() == null) {
            LOG.error("updateAction: Invalid tunnelDest for NodeId: {}", tunnelDestNodeId);
            return null;
        }

        RenderedServicePathHop firstRspHop = renderedServicePath.getRenderedServicePathHop().get(0);
        RenderedServicePathHop lastRspHop = Iterables.getLast(renderedServicePath.getRenderedServicePathHop());
        SfcNshHeader sfcNshHeader = new SfcNshHeaderBuilder().setNshTunIpDst(rspFirstHop.getIp().getIpv4Address())
            .setNshTunUdpPort(rspFirstHop.getPort())
            .setNshNsiToChain(firstRspHop.getServiceIndex())
            .setNshNspToChain(renderedServicePath.getPathId())
            .setNshNsiFromChain((short) (lastRspHop.getServiceIndex().intValue() - 1))
            .setNshNspFromChain(renderedServicePath.getPathId())
            .setNshMetaC1(SfcNshHeader.convertIpAddressToLong(tunnelDest.getIpv4Address()))
            .setNshMetaC2(returnVnid)
            .build();

        // Cannot set all actions here. Some actions are destination specific, and we don't know
        // a destination is to be
        // chained until we reach this point. Need to write match/action in External Table for
        // chained packets.
        actions = addActionBuilder(actions, nxSetNsiAction(sfcNshHeader.getNshNsiToChain()), order);
        actions = addActionBuilder(actions, nxSetNspAction(sfcNshHeader.getNshNspToChain()), order);
        boolean swap=false;
        if ((direction.equals(Direction.Out) && !(isSrcEpConsumer(netElements.getSrcEp(), policyPair.getConsumerEpgId(), ctx)))
         || (direction.equals(Direction.In) && (isSrcEpConsumer(netElements.getSrcEp(), policyPair.getConsumerEpgId(), ctx)))){
            swap = true;
        }
        createChainTunnelFlows(sfcNshHeader, netElements, flowMap, ctx, swap);
        return actions;
    }

    private boolean usesReversePath(Direction direction, PolicyPair policyPair, NetworkElements netElements, OfContext ctx) {
        boolean isConsumer;
        isConsumer = isSrcEpConsumer(netElements.getSrcEp(), policyPair.getConsumerEpgId(), ctx);
        if (isConsumer && direction.equals(Direction.In)) {
            return true;
        }
        if (!isConsumer && direction.equals(Direction.Out)) {
            return true;
        }
        return false;
    }

    public static boolean isSrcEpConsumer(Endpoint srcEp, int consumerEpgOrdId, OfContext ctx ){
        for (EgKey egKey: ctx.getEndpointManager().getEgKeysForEndpoint(srcEp)) {
            try {
                if(OrdinalFactory.getContextOrdinal(egKey.getTenantId(),egKey.getEgId()) == consumerEpgOrdId) {
                    return true;
                }
            } catch (Exception e) {
                LOG.error("isSrcEpConsumer: Could not determine if srcEp {} was consumer", srcEp.getKey());
                return false;
            }
        }
        return false;
    }

    private RenderedServicePath createRsp(ServiceFunctionPath sfcPath, String rspName) {
        CreateRenderedPathInput rspInput = new CreateRenderedPathInputBuilder().setParentServiceFunctionPath(
                sfcPath.getName())
            .setName(rspName)
            .setSymmetric(sfcPath.isSymmetric())
            .build();
         return SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfcPath, rspInput);
    }

    private RenderedServicePath createSymmetricRsp(RenderedServicePath rsp) {
        if (rsp == null) {
            return null;
        }
        return SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(rsp);
    }

    private boolean isValidRspFirstHop(RenderedServicePathFirstHop rspFirstHop) {
        boolean valid = true;
        if (rspFirstHop == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop is null.");
            return false;
        }
        if (rspFirstHop.getIp() == null || rspFirstHop.getIp().getIpv4Address() == null
                || rspFirstHop.getIp().getIpv6Address() != null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has invalid IP address.");
            valid = false;
        }
        if (rspFirstHop.getPort() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no IP port .");
            valid = false;
        }
        if (rspFirstHop.getPathId() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no Path Id (NSP).");
            valid = false;
        }
        if (rspFirstHop.getStartingIndex() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no Starting Index (NSI)");
            valid = false;
        }
        return valid;
    }

    private RenderedServicePath getRspByName(String rspName, ReadOnlyTransaction rTx) {
        Optional<RenderedServicePath> optRsp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                SfcIidFactory.rspIid(rspName), rTx);
        if (optRsp.isPresent()) {
            return optRsp.get();
        }
        return null;
    }

    @Override
    public boolean isValid(ActionInstance actionInstance) {
        return isValidGbpChain(actionInstance.getParameterValue());
    }

    private boolean isValidGbpChain(List<ParameterValue> paramValue) {
        ParameterValue pv = getChainNameParameter(paramValue);
        if (pv == null) {
            return false;
        }
        LOG.trace("isValidGbpChain: Invoking RPC for chain {}", pv.getStringValue());
        ServiceFunctionChain chain = SfcProviderServiceChainAPI.readServiceFunctionChain(pv.getStringValue());
        return (chain != null);
    }

    public ServiceFunctionPath getSfcPath(String chainName) {
        ServiceFunctionPaths paths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        for (ServiceFunctionPath path : paths.getServiceFunctionPath()) {
            if (path.getServiceChainName().equals(chainName)) {
                return path;
            }
        }
        return null;
    }

    private ParameterValue getChainNameParameter(List<ParameterValue> paramValueList) {
        if (paramValueList == null)
            return null;
        for (ParameterValue pv : paramValueList) {
            if (pv.getName().getValue().equals(SFC_CHAIN_NAME)) {
                return pv;
            }
        }
        return null;
    }

    private List<ActionBuilder> addActionBuilder(List<ActionBuilder> actions,
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action action, Integer order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(action);
        ab.setOrder(order);
        actions.add(ab);
        return actions;
    }

}
