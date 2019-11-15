package com.codig.CyberPotato.utils;

import android.text.InputFilter;
import android.text.Spanned;

public class PositiveNumberMaxInputFilter implements InputFilter {

    private final int decimalDigits;//小数的位数
    private final double max;//最大的数值
    private final String unit;//单位

    public PositiveNumberMaxInputFilter(int decimalDigits, double max,String unit) {
        this.decimalDigits = decimalDigits;
        this.max=max;
        if(unit!=null)
            this.unit=unit;
        else
            this.unit="";
    }

    /**
     *
     * @param source 输入时存放新内容，删除时为空
     * @param start 新输入的内容起始下标，输入时和删除时都为0
     * @param end 输入时为新输入内容结束下标-1，其实相当于新输入的字数
     * @param dest 原始的字符串，删除时为空
     * @param dstart 输入时为光标在原始字符中的开始位置，删除时为删除结束时光标所在的位置
     * @param dend 输入时为光标在原始字符中的结束位置，删除时为删除开始时光标所在的位置
     * @return
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String strSource=source.toString();//新输入的字符
        String unchangedFront=dest.subSequence(0,dstart).toString();//未被替换的字符1
        String replaceDest=dest.subSequence(dstart,dend).toString();//被替换前的字符
        String replaceSource=source.toString();//用来替换的字符
        String unchangedEnd=dest.subSequence(dend,dest.length()).toString();//未被替换的字符2
        String result=unchangedFront+replaceSource+unchangedEnd;//替换后的结果
        //必须允许删除键的输入
        if(strSource.equals(""))
            return "";
        //开头为0，接下来只能输入小数点
        if(result.startsWith("0")) {
            if(result.length()>1&&result.charAt(1)!='.')
                return replaceDest;
        }
        try {
            double num;
            if(result.endsWith(unit)) {
                //小数点位数限制
                String[] tempArr=result.substring(0,result.length()-unit.length()).split("[.]");
                if (tempArr.length>=2&&decimalDigits != 0 && tempArr[1].length() > decimalDigits)
                    return replaceDest;
                num = Double.parseDouble(result.substring(0, result.length() - unit.length()));
            }
            else {
                //小数点位数限制
                String[] tempArr=result.split("[.]");
                if (tempArr.length>=2&&decimalDigits != 0 && tempArr[1].length() > decimalDigits)
                    return replaceDest;
                num = Double.parseDouble(result);
            }
            //TODO 限制最小输入
            //数值大小限制
            if(num>max)
                return replaceDest;

        }catch(NumberFormatException e)
        {
            //非正确的数字格式
            return replaceDest;
        }
        return null;
    }
}
