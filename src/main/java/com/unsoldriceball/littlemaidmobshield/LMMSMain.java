package com.unsoldriceball.littlemaidmobshield;


import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.vecmath.Vector2d;
import java.util.Random;

import static com.unsoldriceball.littlemaidmobshield.LMMSMain.ID_MOD;




@Mod(modid = ID_MOD, acceptableRemoteVersions = "*")
public class LMMSMain
{
    public static final String ID_MOD = "littlemaidmobshield";
    private static final String CLASSNAME_MAID = "EntityLittleMaidAvatarMP";


    //ModがInitializeを呼び出す前に発生するイベント。
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        //これでこのクラス内でForgeのイベントが動作するようになるらしい。
        MinecraftForge.EVENT_BUS.register(this);
    }


    //Entityが攻撃を受けたときのイベント。
    @SubscribeEvent
    public void onEntityHurt(LivingHurtEvent event)
    {
        //現在の処理がサーバー側かつダメージを受けたエンティティが存在するか
        if (event.getEntity().world.isRemote) return;
        if (event.getEntityLiving() != null)
        {

            final DamageSource _SOURCE = event.getSource();                           // ダメージを与えた元を取得
            final EntityLivingBase _VICTIM = event.getEntityLiving();                 // ダメージを受けたエンティティを取得


            //ダメージを与えた大元のエンティティが存在し、_VICTIMがLittleMaidで、かつ攻撃がガードできるもの出会った場合。
            if (_SOURCE.getTrueSource() != null && !_SOURCE.isUnblockable() && _VICTIM.getClass().toString().endsWith(CLASSNAME_MAID))
            {
                final ItemStack _VICTIM_MAINHAND = _VICTIM.getHeldItemMainhand();         // _VICTIMのメインハンドアイテム。
                final ItemStack _VICTIM_OFFHAND = _VICTIM.getHeldItemOffhand();           // _VICTIMのオフハンドアイテム。

                ItemStack _VICTIM_SHIELD = null;       //_VICTIMが盾を装備していた場合はここに代入する。


                if (_VICTIM_MAINHAND.getItem() instanceof ItemShield)
                {
                    _VICTIM_SHIELD = _VICTIM_MAINHAND;
                }
                else if (_VICTIM_OFFHAND.getItem() instanceof ItemShield)
                {
                    _VICTIM_SHIELD = _VICTIM_OFFHAND;
                }

                //_VICTIMが盾を装備していた場合。
                if (_VICTIM_SHIELD != null)
                {
                    final Vec3d _ATTACKER_POSITION = getAttackerPos(_SOURCE);

                    //_ATTACKERが_VICTIMの前方にいた場合
                    if (_ATTACKER_POSITION != null && isFront(_ATTACKER_POSITION, _VICTIM))
                    {
                        final boolean _IS_SHIELD_DAMAGEABLE = _VICTIM_SHIELD.isItemStackDamageable();

                        //_VICTIMの盾の最大耐久値とconfigの設定から、防御が成立する確率を出す。(耐久値が存在しない場合は無条件で成立させる。)
                        if (!_IS_SHIELD_DAMAGEABLE || isGuardSuccessed(_VICTIM_SHIELD.getMaxDamage()))
                        {
                            final float _DAMAGEAMOUNT = event.getAmount();

                            //耐久値減少     (minecraftの仕様で、3以下のダメージでは耐久値が減少しない。)
                            if (_DAMAGEAMOUNT > 3 && _IS_SHIELD_DAMAGEABLE)
                            {
                                final int _CEILED_DAMAGEAMOUNT = (int) Math.ceil(_DAMAGEAMOUNT);
                                _VICTIM_SHIELD.damageItem(_CEILED_DAMAGEAMOUNT, _VICTIM);
                            }

                            event.setCanceled(true);  //ダメージ無効化。
                            playBlockingSound(_VICTIM);
                        }
                    }
                }
            }
        }
    }



    //盾の最大耐久値とconfigの設定から、防御が成立する確率を出す
    private boolean isGuardSuccessed(int maxShieldDurability)
    {
        final int _CONFIG_VAlUE = LMMSConfig.requireShieldDurabilityForInvincible;  //盾の最大耐久値がこの値以上であれば100%防御に成功する。

        if (maxShieldDurability >= _CONFIG_VAlUE)
        {
            return true;
        }
        else
        {
            final float _PROBABILITY = ((float) maxShieldDurability / _CONFIG_VAlUE) * 100.0f;
            final int _VALUE = (new Random()).nextInt(101);

            return _PROBABILITY > _VALUE;
        }
    }



    //DamageSourceから、ダメージの発生源の座標を取得する。
    private Vec3d getAttackerPos(DamageSource source)
    {
        final Vec3d _DAMAGE_LOCATION = source.getDamageLocation();      //ダメージの発生源の座標
        final Entity _ATTACKER_SOURCE = source.getImmediateSource();    //ダメージを与えたEntity(矢など)
        final Entity _ATTACKER = source.getTrueSource();                //ダメージを与えた大元のEntity

        Vec3d _attacker_location = null;    //上の3つから得られる座標のうち、nullでないものを代入する。

        if (_DAMAGE_LOCATION != null)
        {
            _attacker_location = _DAMAGE_LOCATION;
        }
        else if (_ATTACKER_SOURCE != null)
        {
            _attacker_location = _ATTACKER_SOURCE.getPositionVector();
        }
        else if (_ATTACKER != null)
        {
            _attacker_location = _ATTACKER.getPositionVector();
        }

        return _attacker_location;
    }



    //locがentityの前方180度に存在しているかを返す。(Y座標は考慮しない。)
    private boolean isFront(Vec3d loc, EntityLivingBase entity)
    {
        final Vector2d _LOC_A = new Vector2d(loc.x, loc.z);
        final Vector2d _LOC_B = new Vector2d(entity.posX, entity.posZ);
        final Vec3d _FACING_B_3D = entity.getLookVec();
        final Vector2d _FACING_B = new Vector2d(_FACING_B_3D.x, _FACING_B_3D.z);

        _FACING_B.normalize();
        final Vector2d _VECTOR_BA = new Vector2d(_LOC_A.x - _LOC_B.x, _LOC_A.y - _LOC_B.y);

        //内積
        return (_VECTOR_BA.x * _FACING_B.x * _VECTOR_BA.y * _VECTOR_BA.y) >= 0;

    }



    //盾でガードしたときの音を鳴らす。
    private void playBlockingSound(EntityLivingBase entity)
    {
        float _VOLUME = 1.0f;
        float _PITCH = ((new Random()).nextInt(11) + 5) * 0.1f;   //0.5～1.5までの乱数を生成する。
        entity.world.playSound(null, entity.getPosition(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, _VOLUME, _PITCH);
    }
}
