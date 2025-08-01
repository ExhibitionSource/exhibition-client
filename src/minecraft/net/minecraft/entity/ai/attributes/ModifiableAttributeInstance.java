package net.minecraft.entity.ai.attributes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ModifiableAttributeInstance implements IAttributeInstance
{
    /** The BaseAttributeMap this attributeInstance can be found in */
    private final BaseAttributeMap attributeMap;

    /** The Attribute this is an instance of */
    private final IAttribute genericAttribute;
    private final Map<Integer, Set<AttributeModifier>> mapByOperation = Maps.<Integer, Set<AttributeModifier>>newHashMap();
    private final Map<String, Set<AttributeModifier>> mapByName = Maps.<String, Set<AttributeModifier>>newHashMap();
    private final Map<UUID, AttributeModifier> mapByUUID = Maps.<UUID, AttributeModifier>newHashMap();
    private double baseValue;
    private boolean needsUpdate = true;
    private double cachedValue;

    public ModifiableAttributeInstance(BaseAttributeMap attributeMapIn, IAttribute genericAttributeIn)
    {
        this.attributeMap = attributeMapIn;
        this.genericAttribute = genericAttributeIn;
        this.baseValue = genericAttributeIn.getDefaultValue();

        for (int i = 0; i < 3; ++i)
        {
            this.mapByOperation.put(Integer.valueOf(i), Sets.<AttributeModifier>newHashSet());
        }
    }

    /**
     * Get the Attribute this is an instance of
     */
    public IAttribute getAttribute()
    {
        return this.genericAttribute;
    }

    public double getBaseValue()
    {
        return this.baseValue;
    }

    public void setBaseValue(double baseValue)
    {
        if (baseValue != this.getBaseValue())
        {
            this.baseValue = baseValue;
            this.flagForUpdate();
        }
    }

    public Collection<AttributeModifier> getModifiersByOperation(int operation)
    {
        return (Collection)this.mapByOperation.get(Integer.valueOf(operation));
    }

    public Collection<AttributeModifier> func_111122_c_getModifierListCopy()
    {
        Set<AttributeModifier> set = Sets.<AttributeModifier>newHashSet();

        for (int i = 0; i < 3; ++i)
        {
            set.addAll(this.getModifiersByOperation(i));
        }

        return set;
    }

    /**
     * Returns attribute modifier, if any, by the given UUID
     */
    public AttributeModifier getModifier(UUID uuid)
    {
        return (AttributeModifier)this.mapByUUID.get(uuid);
    }

    public boolean hasModifier(AttributeModifier modifier)
    {
        return this.mapByUUID.get(modifier.getID()) != null;
    }

    public void applyModifier(AttributeModifier modifier)
    {
        if (this.getModifier(modifier.getID()) != null)
        {
            throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        }
        else
        {
            Set<AttributeModifier> set = (Set)this.mapByName.get(modifier.getName());

            if (set == null)
            {
                set = Sets.<AttributeModifier>newHashSet();
                this.mapByName.put(modifier.getName(), set);
            }

            ((Set)this.mapByOperation.get(Integer.valueOf(modifier.getOperation()))).add(modifier);
            set.add(modifier);
            this.mapByUUID.put(modifier.getID(), modifier);
            this.flagForUpdate();
        }
    }

    protected void flagForUpdate()
    {
        this.needsUpdate = true;
        this.attributeMap.func_180794_a(this);
    }

    public void removeModifier(AttributeModifier modifier)
    {
        for (int i = 0; i < 3; ++i)
        {
            Set<AttributeModifier> set = (Set)this.mapByOperation.get(Integer.valueOf(i));
            set.remove(modifier);
        }

        Set<AttributeModifier> set1 = (Set)this.mapByName.get(modifier.getName());

        if (set1 != null)
        {
            set1.remove(modifier);

            if (set1.isEmpty())
            {
                this.mapByName.remove(modifier.getName());
            }
        }

        this.mapByUUID.remove(modifier.getID());
        this.flagForUpdate();
    }

    public void removeAllModifiers()
    {
        Collection<AttributeModifier> collection = this.func_111122_c_getModifierListCopy();

        if (collection != null)
        {
            for (AttributeModifier attributemodifier : Lists.newArrayList(collection))
            {
                this.removeModifier(attributemodifier);
            }
        }
    }

    public double getAttributeValue()
    {
        if (this.needsUpdate)
        {
            this.cachedValue = this.computeValue();
            this.needsUpdate = false;
        }

        return this.cachedValue;
    }

    private double computeValue()
    {
        double d0 = this.getBaseValue();

        for (AttributeModifier attributemodifier : this.func_180375_b_getModifiersByOperation(0))
        {
            d0 += attributemodifier.getAmount();
        }

        double d1 = d0;

        for (AttributeModifier attributemodifier1 : this.func_180375_b_getModifiersByOperation(1))
        {
            d1 += d0 * attributemodifier1.getAmount();
        }

        for (AttributeModifier attributemodifier2 : this.func_180375_b_getModifiersByOperation(2))
        {
            d1 *= 1.0D + attributemodifier2.getAmount();
        }

        return this.genericAttribute.clampValue(d1);
    }

    private Collection<AttributeModifier> func_180375_b_getModifiersByOperation(int p_180375_1_)
    {
        Set<AttributeModifier> set = Sets.newHashSet(this.getModifiersByOperation(p_180375_1_));

        for (IAttribute iattribute = this.genericAttribute.func_180372_d(); iattribute != null; iattribute = iattribute.func_180372_d())
        {
            IAttributeInstance iattributeinstance = this.attributeMap.getAttributeInstance(iattribute);

            if (iattributeinstance != null)
            {
                set.addAll(iattributeinstance.getModifiersByOperation(p_180375_1_));
            }
        }

        return set;
    }
}
