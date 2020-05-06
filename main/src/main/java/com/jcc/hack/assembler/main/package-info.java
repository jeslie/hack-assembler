/**
 * Hack Assembler program (command line interface).
 * <p>
 * An enhanced implementation of the two pass <a
 * href="https://b1391bd6-da3d-477d-8c01-38cdf774495a.filesusr.com/ugd/44046b_89a8e226476741a3b7c5204575b8a0b2.pdf">
 * Hack Assembler</a>. This implementation supports the following additional
 * features:
 * </p>
 * <ul>
 *     <li>options (including source listing, object listing, symbol table dump,
 *     filename resolution display)</li>
 *     <li>self-help</li>
 *     <li>specification of the location/name of the generated Hack machine
 *     language file</li>
 *     <li>allowing embedded whitespace between C-command fields and their
 *     delimiters</li>
 *     <li>extensive error checks</li>
 * </ul>
 * <p>
 *     There is no <a
 *     href="https://whatis.techtarget.com/definition/internationalization-I18N">
 *     I18N</a> support.
 *     All output text is in English.
 * </p>
 * <p>
 *     <b>N.B.</b>: Unless explicitly stated otherwise, all method and
 *     constructor parameters are presumed to be non-{@code null}.
 * </p>
 *
 * @version 1.0
 * @author Jeslie Chermak
 * @see
 * <a href="https://www.amazon.com/Elements-Computing-Systems-Building-Principles/dp/0262640686/ref=ed_oe_p">
 *     The Elements of Computing Systems</a>
 * @see
 * <a href="https://b1391bd6-da3d-477d-8c01-38cdf774495a.filesusr.com/ugd/44046b_7ef1c00a714c46768f08c459a6cab45a.pdf">
 *     Chapter 4: Machine Language</a>
 * @see
 * <a href="https://b1391bd6-da3d-477d-8c01-38cdf774495a.filesusr.com/ugd/44046b_7ef1c00a714c46768f08c459a6cab45a.pdf">
 *     Chapter 6: Assembler</a>
 * @see <a href="http://cit.cs.dixie.edu/cs/2810/hack.pdf">Hack Assembly
 *     Language</a>
 */
package com.jcc.hack.assembler.main;

