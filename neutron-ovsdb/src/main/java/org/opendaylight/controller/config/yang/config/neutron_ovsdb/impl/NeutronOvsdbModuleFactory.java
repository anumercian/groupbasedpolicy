/*
* Generated file
*
* Generated from: yang module name: neutron-ovsdb-impl yang module local name: neutron-ovsdb-impl
* Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
* Generated at: Fri May 15 13:05:35 EDT 2015
*
* Do not modify this file unless it is present under src/main directory
*/
package org.opendaylight.controller.config.yang.config.neutron_ovsdb.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;

public class NeutronOvsdbModuleFactory extends org.opendaylight.controller.config.yang.config.neutron_ovsdb.impl.AbstractNeutronOvsdbModuleFactory {

    /**
     * @see org.opendaylight.controller.config.yang.config.neutron_mapper.impl.AbstractNeutronOvsdbModuleFactory#createModule(java.lang.String, org.opendaylight.controller.config.api.DependencyResolver, org.osgi.framework.BundleContext)
     */
    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        NeutronOvsdbModule module = (NeutronOvsdbModule) super.createModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    /**
     * @see org.opendaylight.controller.config.yang.config.neutron_mapper.impl.AbstractNeutronOvsdbModuleFactory#createModule(java.lang.String, org.opendaylight.controller.config.api.DependencyResolver, org.opendaylight.controller.config.api.DynamicMBeanWithInstance, org.osgi.framework.BundleContext)
     */
    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, DynamicMBeanWithInstance old,
            BundleContext bundleContext) throws Exception {
        NeutronOvsdbModule module = (NeutronOvsdbModule) super.createModule(instanceName, dependencyResolver, old, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }


}