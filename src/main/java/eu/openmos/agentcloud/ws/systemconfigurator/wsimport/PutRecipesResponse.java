
package eu.openmos.agentcloud.ws.systemconfigurator.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import eu.openmos.agentcloud.utilities.ServiceCallStatus;


/**
 * <p>Classe Java per putRecipesResponse complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="putRecipesResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="serviceCallStatus" type="{http://cloudinterface.agentcloud.openmos.eu/}serviceCallStatus" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "putRecipesResponse", propOrder = {
    "serviceCallStatus"
})
public class PutRecipesResponse {

    protected ServiceCallStatus serviceCallStatus;

    /**
     * Recupera il valore della proprietà serviceCallStatus.
     * 
     * @return
     *     possible object is
     *     {@link ServiceCallStatus }
     *     
     */
    public ServiceCallStatus getServiceCallStatus() {
        return serviceCallStatus;
    }

    /**
     * Imposta il valore della proprietà serviceCallStatus.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceCallStatus }
     *     
     */
    public void setServiceCallStatus(ServiceCallStatus value) {
        this.serviceCallStatus = value;
    }

}
