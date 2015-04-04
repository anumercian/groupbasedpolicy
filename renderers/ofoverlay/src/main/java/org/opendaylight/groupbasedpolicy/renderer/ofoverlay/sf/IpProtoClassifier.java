/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Match on the IP protocol of IP traffic
 */
public class IpProtoClassifier extends Classifier {

    public static final Long TCP = Long.valueOf(6);
    public static final Long UDP = Long.valueOf(17);
    public static final Long SCTP = Long.valueOf(132);
    public static final String PROTO = "proto";
    public static final ClassifierDefinitionId ID = new ClassifierDefinitionId("79c6fdb2-1e1a-4832-af57-c65baf5c2335");
    protected static final ClassifierDefinition DEF = new ClassifierDefinitionBuilder().setId(ID)
        .setParent(EtherTypeClassifier.ID)
        .setName(new ClassifierName("ip_proto"))
        .setDescription(new Description("Match on the IP protocol of IP traffic"))
        .setParameter(
                ImmutableList.of(new ParameterBuilder().setName(new ParameterName(PROTO))
                    .setDescription(new Description("The IP protocol to match against"))
                    .setIsRequired(IsRequired.Required)
                    .setType(Type.Int)
                    .build()))
        .build();

    protected IpProtoClassifier(Classifier parent) {
        super(parent);
    }

    @Override
    public ClassifierDefinitionId getId() {
        return ID;
    }

    @Override
    public ClassifierDefinition getClassDef() {
        return DEF;
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        if (params.get(PROTO) == null) {
            throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                    + "}+ Parameter proto not present.");
        }
        if (params.get(PROTO).getIntValue() == null) {
            throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                    + "}+ Value of proto parameter is not present.");
        }
    }

    @Override
    protected List<MatchBuilder> update(List<MatchBuilder> matches, Map<String, ParameterValue> params) {
        Long proto = params.get(PROTO).getIntValue();
        for (MatchBuilder match : matches) {
            IpMatchBuilder imb;
            if (match.getIpMatch() != null) {
                equalOrNotSetValidation(match.getIpMatch().getIpProtocol(), proto);
                continue;
            } else {
                imb = new IpMatchBuilder();
            }
            imb.setIpProtocol(proto.shortValue());
            match.setIpMatch(imb.build());
        }
        return matches;
    }

    private void equalOrNotSetValidation(Short protoInMatch, long paramValue) {
        if (protoInMatch != null) {
            if (paramValue != protoInMatch.longValue()) {
                throw new IllegalArgumentException("Classification conflict at " + this.getClassDef().getName()
                        + ": Trying to override proto value: " + protoInMatch.shortValue() + " by value " + paramValue);
            }
        }
    }

    @Override
    protected void checkPrereqs(List<MatchBuilder> matches) {
        for (MatchBuilder match : matches) {
            Long readEthType = null;
            try {
                readEthType = match.getEthernetMatch().getEthernetType().getType().getValue();
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("Ether-type match is missing.");
            }
            if (!FlowUtils.IPv4.equals(readEthType) && !FlowUtils.IPv6.equals(readEthType)) {
                throw new IllegalArgumentException("Ether-type value should be " + FlowUtils.IPv4 + " or "
                        + FlowUtils.IPv6 + ".");
            }
        }
    }

    /**
     * May return null.
     */
    public static Long getIpProtoValue(Map<String, ParameterValue> params) {
        if (params == null) {
            return null;
        }
        if (params.get(PROTO) == null) {
            return null;
        }
        Long proto = params.get(PROTO).getIntValue();
        if (proto != null) {
            return proto;
        }
        return null;
    }
}