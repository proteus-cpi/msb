
package eu.openmos.agentcloud.ws.systemconfigurator.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import eu.openmos.model.Skill;


/**
 * <p>Java class for createNewSkill complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="createNewSkill">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="skill" type="{http://cloudinterface.agentcloud.openmos.eu/}skill" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "createNewSkill", propOrder = {
    "skill"
})
public class CreateNewSkill {

    protected Skill skill;

    /**
     * Gets the value of the skill property.
     * 
     * @return
     *     possible object is
     *     {@link Skill }
     *     
     */
    public Skill getSkill() {
        return skill;
    }

    /**
     * Sets the value of the skill property.
     * 
     * @param value
     *     allowed object is
     *     {@link Skill }
     *     
     */
    public void setSkill(Skill value) {
        this.skill = value;
    }

}
